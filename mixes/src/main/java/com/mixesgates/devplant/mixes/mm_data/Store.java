package com.mixesgates.devplant.mixes.mm_data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Plant;
import com.mixesgates.devplant.contracts.ScanOutcome;
import com.mixesgates.devplant.contracts.ScanRequest;
import com.mixesgates.devplant.contracts.Services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The screens' only way to the data. Runs contract calls off the main thread and hands results
 * back on it, dropping them if the screen has gone.
 */
public final class Store {

    public interface Work<T> {
        T run(Services services) throws Exception;
    }

    public interface Done<T> {
        void done(T value);
    }

    private static final String TAG = "Store";
    private static Store instance;

    public static synchronized Store of(Context context) {
        if (instance == null) {
            Context app = context.getApplicationContext();
            if (!(app instanceof Services.Provider)) {
                throw new IllegalStateException("The Application must implement Services.Provider");
            }
            instance = new Store(app, ((Services.Provider) app).services());
        }
        return instance;
    }

    private final Context app;
    private final Services services;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService scanner = Executors.newSingleThreadExecutor();
    private final ExecutorService downloads = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    public final PhotoCache photos = new PhotoCache();

    private Store(Context app, Services services) {
        this.app = app;
        this.services = services;
    }

    /** Reads on a background thread; {@code done} runs on the main thread while the owner lives. */
    public <T> void load(LifecycleOwner owner, Work<T> work, Done<T> done) {
        submit(io, owner, work, done, this::showError);
    }

    /** Like {@link #load(LifecycleOwner, Work, Done)}, but the screen handles the failure itself. */
    public <T> void load(LifecycleOwner owner, Work<T> work, Done<T> done, Done<Throwable> failed) {
        submit(io, owner, work, done, failed);
    }

    /** Shows a catalogue plant's photo: downloaded the first time, read from the phone after that. */
    public void plantPhoto(ImageView view, Plant plant, int sizePx) {
        String tag = "plant:" + plant.key;
        view.setTag(tag);
        view.setImageDrawable(null);
        downloads.execute(() -> {
            String path = services.plants().photoFile(plant);
            main.post(() -> {
                if (path != null && tag.equals(view.getTag())) photos.load(view, path, sizePx);
            });
        });
    }

    /** Writes on a background thread; change events tell screens to refresh. */
    public void write(Work<?> work) {
        io.execute(() -> {
            try {
                work.run(services);
            } catch (Throwable t) {
                Log.e(TAG, "write failed", t);
                main.post(() -> showError(t));
            }
        });
    }

    /** Forwards a photo in the app cache to ScanStore, then deletes the cached copy. */
    public void scan(LifecycleOwner owner, File photo, long fieldId, Done<ScanOutcome> done,
                     Done<Throwable> failed) {
        submit(scanner, owner, s -> {
            try {
                return s.scans().scan(new ScanRequest(photo.getPath(), fieldId));
            } finally {
                //noinspection ResultOfMethodCallIgnored
                photo.delete();
            }
        }, done, failed);
    }

    /** Same as {@link #scan(LifecycleOwner, File, long, Done, Done)} for a picked gallery photo. */
    public void scan(LifecycleOwner owner, Uri picked, long fieldId, Done<ScanOutcome> done,
                     Done<Throwable> failed) {
        submit(scanner, owner, s -> {
            File copy = copyToCache(picked);
            try {
                return s.scans().scan(new ScanRequest(copy.getPath(), fieldId));
            } finally {
                //noinspection ResultOfMethodCallIgnored
                copy.delete();
            }
        }, done, failed);
    }

    /** Runs {@code onChange} on the main thread whenever one of the topics changes. */
    public void listen(LifecycleOwner owner, Runnable onChange, Changes.Topic... topics) {
        Set<Changes.Topic> wanted = EnumSet.noneOf(Changes.Topic.class);
        for (Changes.Topic t : topics) wanted.add(t);
        Changes.Listener listener = topic -> {
            if (wanted.contains(topic)) main.post(() -> {
                if (alive(owner)) onChange.run();
            });
        };
        services.changes().listen(listener);
        owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner o) {
                services.changes().unlisten(listener);
            }
        });
    }

    private <T> void submit(Executor executor, LifecycleOwner owner, Work<T> work, Done<T> done,
                            Done<Throwable> failed) {
        executor.execute(() -> {
            T value;
            try {
                value = work.run(services);
            } catch (Throwable t) {
                Log.e(TAG, "work failed", t);
                main.post(() -> {
                    if (alive(owner)) failed.done(t);
                });
                return;
            }
            main.post(() -> {
                if (alive(owner)) done.done(value);
            });
        });
    }

    private File copyToCache(Uri uri) throws IOException {
        File target = new File(app.getCacheDir(), "picked_" + System.currentTimeMillis() + ".jpg");
        try (InputStream in = app.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IOException("Cannot open " + uri);
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        }
        return target;
    }

    private static boolean alive(LifecycleOwner owner) {
        return owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED);
    }

    private void showError(Throwable t) {
        Toast.makeText(app, "Something went wrong: " + t.getMessage(), Toast.LENGTH_LONG).show();
    }
}
