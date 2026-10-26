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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The plant catalogue from the DevPlant cloud (the cloud/ folder, hosted on Vercel).
 * The first open downloads /api/catalog and keeps it; later opens use the saved copy at once and
 * look for a newer one in the background. Photos are downloaded the first time they are shown.
 */
public final class CloudPlantCatalog implements PlantCatalog {

    private static final String TAG = "CloudPlantCatalog";
    private static final String SAVED = "catalog.json";
    private static final String INCOMING = "catalog.next.json";

    private final WebFiles files;
    private final String baseUrl;
    private final Changes changes;
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

    public CloudPlantCatalog(WebFiles files, String baseUrl, Changes changes) {
        this.files = files;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.changes = changes;
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
    public String photoFile(Plant plant) {
        try {
            return files.cached(plant.imageUrl, "images/" + plant.key + ".jpg").getPath();
        } catch (IOException e) {
            Log.w(TAG, "photo unavailable: " + plant.key + " (" + e.getMessage() + ")");
            return null;
        }
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
            loaded = fetch();
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
            Loaded fresh = fetch();
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

    /** Downloads and checks the catalogue, and only then replaces the saved copy. */
    private Loaded fetch() throws IOException, JSONException {
        files.download(baseUrl + "/api/catalog", INCOMING);
        String json = files.read(INCOMING);
        Loaded fresh = parse(json);
        File target = files.file(SAVED);
        //noinspection ResultOfMethodCallIgnored
        target.delete();
        if (!files.file(INCOMING).renameTo(target)) throw new IOException("Cannot save the catalogue");
        return fresh;
    }

    private Loaded parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        List<String> categories = new ArrayList<>();
        JSONArray cats = root.getJSONArray("categories");
        for (int i = 0; i < cats.length(); i++) categories.add(cats.getString(i));

        List<Plant> plants = new ArrayList<>();
        JSONArray rows = root.getJSONArray("plants");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject p = rows.getJSONObject(i);
            List<PlantProblem> problems = new ArrayList<>();
            JSONArray probs = p.getJSONArray("problems");
            for (int j = 0; j < probs.length(); j++) {
                JSONObject x = probs.getJSONObject(j);
                problems.add(new PlantProblem(x.getString("key"), x.getString("name"),
                        x.getString("signs"), x.getString("treatment")));
            }
            String imageUrl = p.optString("image_url", "");
            if (imageUrl.isEmpty()) imageUrl = baseUrl + "/" + p.getString("image");
            String author = p.optString("image_author", "");
            String credit = p.optString("image_license", "") + (author.isEmpty() ? "" : ", " + author);
            plants.add(new Plant(p.getString("key"), p.getString("name"),
                    p.optString("scientific_name", ""), p.optString("category", ""), imageUrl,
                    credit, p.optString("image_source", ""), problems));
        }
        return new Loaded(json, plants, categories);
    }
}
