package com.mixesgates.devplant.gates.gm_http;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Downloads files over HTTPS and keeps them in app-private storage, so whatever was fetched once
 * is still there with no signal.
 */
public final class WebFiles {

    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private final File dir;

    public WebFiles(Context context) {
        dir = new File(context.getFilesDir(), "cloud");
    }

    /** Downloads {@code url} into the saved file {@code name}, replacing any older copy. */
    public File download(String url, String name) throws IOException {
        fetch(url, name, null);
        return file(name);
    }

    /**
     * Downloads {@code url} into {@code name} unless the server still has version {@code etag}
     * (the ETag from last time, or null). Returns the new version's ETag ("" if the server sent
     * none), or null when nothing changed and nothing was downloaded.
     */
    public String downloadIfChanged(String url, String name, String etag) throws IOException {
        return fetch(url, name, etag);
    }

    /** The saved copy of {@code url}, downloading it only if there is none yet. */
    public File cached(String url, String name) throws IOException {
        File f = file(name);
        return f.isFile() && f.length() > 0 ? f : download(url, name);
    }

    /** The saved copy of {@code name}, or null without touching the network. */
    public File saved(String name) {
        File f = file(name);
        return f.isFile() && f.length() > 0 ? f : null;
    }

    /** Where {@code name} is saved; it may not exist yet. */
    public File file(String name) {
        return new File(dir, name);
    }

    /** The saved file as text, or null if it has never been downloaded. */
    public String read(String name) throws IOException {
        return readFile(file(name));
    }

    /** Saves a small text file, e.g. the version of a download. */
    public void write(String name, String text) throws IOException {
        try (OutputStream out = new FileOutputStream(file(name))) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Returns the ETag ("" if none), or null on 304 Not Modified (only when {@code etag} is given). */
    private String fetch(String url, String name, String etag) throws IOException {
        File target = file(name);
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        File part = new File(target.getPath() + ".part");
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        http.setConnectTimeout(CONNECT_TIMEOUT_MS);
        http.setReadTimeout(READ_TIMEOUT_MS);
        http.setRequestProperty("Accept-Encoding", "gzip");
        if (etag != null) http.setRequestProperty("If-None-Match", etag);
        String newTag;
        try {
            int code = http.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED && etag != null) return null;
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code + " for " + url);
            newTag = http.getHeaderField("ETag");
            InputStream body = http.getInputStream();
            if ("gzip".equalsIgnoreCase(http.getContentEncoding())) {
                body = new GZIPInputStream(body);
            }
            try (InputStream in = body; OutputStream out = new FileOutputStream(part)) {
                byte[] buffer = new byte[64 * 1024];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            }
        } finally {
            http.disconnect();
        }
        if (!part.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            part.delete();
            throw new IOException("Cannot save " + target);
        }
        return newTag == null ? "" : newTag;
    }

    private static String readFile(File f) throws IOException {
        if (!f.isFile()) return null;
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            return out.toString("UTF-8");
        }
    }
}
