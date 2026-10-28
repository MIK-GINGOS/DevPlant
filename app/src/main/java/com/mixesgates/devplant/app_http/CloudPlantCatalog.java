package com.mixesgates.devplant.app_http;

import android.util.Log;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Plant;
import com.mixesgates.devplant.contracts.PlantCatalog;
import com.mixesgates.devplant.contracts.PlantProblem;
import com.mixesgates.devplant.gates.gm_http.WebFiles;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The plant catalogue from the DevPlant cloud (the cloud/ folder, hosted on Vercel).
 * The first open downloads /api/catalog and keeps it; later opens use the saved copy at once and
 * ask the server in the background whether it has a newer one (by ETag, so an unchanged catalogue
 * is not downloaded again). Each photo is downloaded once, the first time it is shown, and read
 * from the phone after that.
 *
 * <p>Some networks can't reach Vercel. Then the catalogue and photos come from the mirror (the same
 * cloud/ folder in the public GitHub repo, through jsDelivr), and for a while photos go to the
 * mirror first instead of waiting for Vercel to time out on every one.
 */
public final class CloudPlantCatalog implements PlantCatalog {

    private static final String TAG = "CloudPlantCatalog";
    private static final String SAVED = "catalog.json";
    private static final String INCOMING = "catalog.next.json";
    /** Where the saved catalogue came from and its ETag, one per line. */
    private static final String VERSION = "catalog.version";
    private static final long CLOUD_DOWN_MS = 10 * 60_000;

    private final WebFiles files;
    private final String baseUrl;
    private final String mirrorUrl;
    private final Changes changes;
    private final Map<String, Object> photoLocks = new ConcurrentHashMap<>();
    private volatile long cloudDownUntil;
    private final ExecutorService refresher = Executors.newSingleThreadExecutor();
    private Loaded loaded;
    private boolean refreshed;

    private static final class Loaded {
        final String json;
        final List<Plant> plants;
        final List<String> categories;
        final Map<String, Plant> byKey;

        Loaded(String json, List<Plant> plants, List<String> categories) {
            this.json = json;
            this.plants = Collections.unmodifiableList(plants);
            this.categories = Collections.unmodifiableList(categories);
            byKey = new HashMap<>();
            for (Plant p : plants) byKey.put(p.key, p);
        }
    }

    public CloudPlantCatalog(WebFiles files, String baseUrl, String mirrorUrl, Changes changes) {
        this.files = files;
        this.baseUrl = trim(baseUrl);
        this.mirrorUrl = trim(mirrorUrl);
        this.changes = changes;
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public List<Plant> plants() throws Exception {
        return loaded().plants;
    }

    @Override
    public List<String> categories() throws Exception {
        return loaded().categories;
    }

    @Override
    public Plant plant(String key) throws Exception {
        return key == null ? null : loaded().byKey.get(key.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<Plant> search(String query, String category) throws Exception {
        String[] words = query == null ? new String[0]
                : query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<Plant> out = new ArrayList<>();
        for (Plant p : loaded().plants) {
            if (category != null && !category.equals(p.category)) continue;
            StringBuilder text = new StringBuilder()
                    .append(p.name).append(' ').append(p.scientificName).append(' ').append(p.category);
            for (PlantProblem x : p.problems) text.append(' ').append(x.name);
            String haystack = text.toString().toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String w : words) {
                if (!w.isEmpty() && !haystack.contains(w)) {
                    all = false;
                    break;
                }
            }
            if (all) out.add(p);
        }
        return out;
    }

    @Override
    public String savedPhotoFile(Plant plant) {
        File f = files.saved(photoName(plant));
        return f == null ? null : f.getPath();
    }

    @Override
    public String photoFile(Plant plant) {
        String name = photoName(plant);
        File saved = files.saved(name);
        if (saved != null) return saved.getPath();
        // One download per photo, even when two screens ask for it at once.
        Object lock = photoLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            saved = files.saved(name);
            if (saved != null) return saved.getPath();
            String tail = photoPath(plant);
            String cloud = baseUrl + tail;
            String mirror = mirrorUrl + "/public" + tail;
            boolean cloudFirst = System.currentTimeMillis() >= cloudDownUntil;
            try {
                return files.download(cloudFirst ? cloud : mirror, name).getPath();
            } catch (IOException first) {
                if (cloudFirst) cloudDownUntil = System.currentTimeMillis() + CLOUD_DOWN_MS;
                try {
                    return files.download(cloudFirst ? mirror : cloud, name).getPath();
                } catch (IOException second) {
                    Log.w(TAG, "photo unavailable: " + plant.key + " (" + second.getMessage() + ")");
                    return null;
                }
            }
        }
    }

    private static String photoName(Plant plant) {
        return "images/" + plant.key + ".jpg";
    }

    /** "/images/tomato.jpg": the photo's path under the site root, the same on Vercel and the mirror. */
    private static String photoPath(Plant plant) {
        int at = plant.imageUrl.lastIndexOf("/images/");
        return at >= 0 ? plant.imageUrl.substring(at) : "/images/" + plant.key + ".jpg";
    }

    @Override
    public String source() {
        return baseUrl;
    }

    /** The saved catalogue, or a fresh download if this phone has never had one. */
    private synchronized Loaded loaded() throws Exception {
        if (loaded != null) return loaded;
        String saved = files.read(SAVED);
        if (saved != null) {
            try {
                loaded = parse(saved);
            } catch (JSONException e) {
                Log.w(TAG, "saved catalogue unreadable, downloading again", e);
            }
        }
        if (loaded == null) {
            loaded = fetch(true);
            refreshed = true;
        } else if (!refreshed) {
            refreshed = true;
            refresher.execute(this::refresh);
        }
        return loaded;
    }

    /** Looks for a newer catalogue; with no signal the saved one simply stays. */
    private void refresh() {
        try {
            Loaded fresh = fetch(false);
            if (fresh == null) return; // the server still has the saved version
            boolean changed;
            synchronized (this) {
                changed = loaded == null || !fresh.json.equals(loaded.json);
                loaded = fresh;
            }
            if (changed) changes.post(Changes.Topic.CATALOG);
        } catch (Exception e) {
            Log.i(TAG, "catalogue not refreshed: " + e.getMessage());
        }
    }

    /**
     * Downloads and checks the catalogue, and only then replaces the saved copy. Tries the cloud,
     * then the mirror. Returns null if the source still has the saved version, unless {@code must}.
     */
    private Loaded fetch(boolean must) throws IOException, JSONException {
        try {
            return fetchFrom(baseUrl + "/api/catalog", must);
        } catch (IOException e) {
            Log.i(TAG, "cloud unreachable, trying the mirror: " + e.getMessage());
            cloudDownUntil = System.currentTimeMillis() + CLOUD_DOWN_MS;
            return fetchFrom(mirrorUrl + "/gates/gm_data/catalog.json", must);
        }
    }

    private Loaded fetchFrom(String url, boolean must) throws IOException, JSONException {
        String version = must ? null : files.read(VERSION);
        String etag = null;
        if (version != null) {
            String[] lines = version.split("\n", 2);
            if (lines.length == 2 && lines[0].equals(url) && !lines[1].isEmpty()) etag = lines[1];
        }
        String newTag = files.downloadIfChanged(url, INCOMING, etag);
        if (newTag == null) return null;
        Loaded fresh = parse(files.read(INCOMING));
        File target = files.file(SAVED);
        //noinspection ResultOfMethodCallIgnored
        target.delete();
        if (!files.file(INCOMING).renameTo(target)) throw new IOException("Cannot save the catalogue");
        files.write(VERSION, url + "\n" + newTag);
        return fresh;
    }

    /**
     * Reads either shape: the API's (categories, and plants with their problems) or the data file
     * from the mirror (diseases listed once, plants naming theirs by key).
     */
    private Loaded parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        Map<String, JSONObject> diseases = null;
        if (root.has("diseases")) {
            diseases = new HashMap<>();
            JSONArray all = root.getJSONArray("diseases");
            for (int i = 0; i < all.length(); i++) {
                JSONObject d = all.getJSONObject(i);
                diseases.put(d.getString("key"), d);
            }
        }
        List<String> categories = new ArrayList<>();
        JSONArray cats = root.optJSONArray("categories");
        if (cats != null) for (int i = 0; i < cats.length(); i++) categories.add(cats.getString(i));

        List<Plant> plants = new ArrayList<>();
        JSONArray rows = root.getJSONArray("plants");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject p = rows.getJSONObject(i);
            List<PlantProblem> problems = new ArrayList<>();
            if (diseases == null) {
                JSONArray probs = p.getJSONArray("problems");
                for (int j = 0; j < probs.length(); j++) problems.add(problem(probs.getJSONObject(j)));
            } else {
                JSONArray keys = p.getJSONArray("diseases");
                for (int j = 0; j < keys.length(); j++) {
                    JSONObject d = diseases.get(keys.getString(j));
                    if (d != null) problems.add(problem(d));
                }
            }
            String category = p.optString("category", "");
            if (cats == null && !categories.contains(category)) categories.add(category);
            String imageUrl = p.optString("image_url", "");
            if (imageUrl.isEmpty()) {
                imageUrl = (diseases == null ? baseUrl : mirrorUrl + "/public") + "/" + p.getString("image");
            }
            String author = p.optString("image_author", "");
            String credit = p.optString("image_license", "") + (author.isEmpty() ? "" : ", " + author);
            plants.add(new Plant(p.getString("key"), p.getString("name"),
                    p.optString("scientific_name", ""), category, imageUrl,
                    credit, p.optString("image_source", ""), problems));
        }
        return new Loaded(json, plants, categories);
    }

    private static PlantProblem problem(JSONObject x) throws JSONException {
        return new PlantProblem(x.getString("key"), x.getString("name"),
                x.getString("signs"), x.getString("treatment"));
    }
}
