package com.mixesgates.devplant.app_reports;

import android.util.Log;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Report;
import com.mixesgates.devplant.gates.gm_http.JsonHttp;
import com.mixesgates.devplant.gates.gm_reports.ConsentPrefs;
import com.mixesgates.devplant.gates.gm_reports.QuestionRow;
import com.mixesgates.devplant.gates.gm_reports.ReportRow;
import com.mixesgates.devplant.gates.gm_reports.ReportsDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Talks to the officer service (cloud/api/reports, questions, updates): sends queued reports and
 * questions, then fetches what officers set on them. Report details only: the photo path is never
 * part of what is sent. With no signal everything simply stays queued for the next try, which
 * comes at the next app start, scan, question, or opening of My reports.
 */
final class ReportSync {

    private static final String TAG = "ReportSync";
    private static final int BATCH = 50;

    private final ReportsDatabase db;
    private final ConsentPrefs consent;
    private final JsonHttp http;
    private final String baseUrl;
    private final Changes changes;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean pending = new AtomicBoolean();

    ReportSync(ReportsDatabase db, ConsentPrefs consent, JsonHttp http, String baseUrl, Changes changes) {
        this.db = db;
        this.consent = consent;
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.changes = changes;
    }

    /** Syncs in the background; several calls in a row make one sync. */
    void soon() {
        if (pending.compareAndSet(false, true)) {
            worker.execute(() -> {
                pending.set(false);
                now();
            });
        }
    }

    /** Syncs on this thread. False when the service could not be reached. */
    synchronized boolean now() {
        boolean changed = false;
        try {
            changed = sendReports();
            changed |= sendQuestions();
            changed |= fetchUpdates();
            return true;
        } catch (IOException | JSONException e) {
            Log.i(TAG, "officer service not reached: " + e.getMessage());
            return false;
        } finally {
            if (changed) changes.post(Changes.Topic.SCANS);
        }
    }

    private Map<String, String> device() {
        return Collections.singletonMap("X-Device-Key", consent.deviceKey());
    }

    private boolean sendReports() throws IOException, JSONException {
        boolean sent = false;
        List<ReportRow> rows;
        while (!(rows = db.reports().queued(BATCH)).isEmpty()) {
            JSONArray list = new JSONArray();
            for (ReportRow r : rows) list.put(reportJson(r));
            String reply = http.post(baseUrl + "/api/reports", new JSONObject().put("reports", list).toString(), device());
            List<Long> accepted = ids(new JSONObject(reply).getJSONArray("accepted"));
            if (accepted.isEmpty()) break;
            db.reports().markSent(accepted);
            sent = true;
        }
        return sent;
    }

    private static JSONObject reportJson(ReportRow r) throws JSONException {
        return new JSONObject()
                .put("id", r.id)
                .put("createdAt", r.createdAt)
                .put("cropName", r.cropName)
                .put("country", r.country)
                .put("district", r.district)
                .put("kind", r.kind)
                .put("resultKey", r.resultKey)
                .put("resultName", r.resultName)
                .put("confidence", (double) r.confidence)
                .put("uncertain", r.scanId == 0)
                .put("uncertainReason", r.uncertainReason)
                .put("source", r.source);
    }

    private boolean sendQuestions() throws IOException, JSONException {
        boolean sent = false;
        List<QuestionRow> rows;
        while (!(rows = db.questions().queued(BATCH)).isEmpty()) {
            JSONArray list = new JSONArray();
            for (QuestionRow q : rows) {
                list.put(new JSONObject()
                        .put("id", q.id)
                        .put("askedAt", q.askedAt)
                        .put("text", q.text)
                        .put("reportId", q.reportId == 0 ? JSONObject.NULL : q.reportId)
                        .put("cropName", q.cropName)
                        .put("country", consent.country())
                        .put("district", consent.district()));
            }
            String reply = http.post(baseUrl + "/api/questions", new JSONObject().put("questions", list).toString(), device());
            List<Long> accepted = ids(new JSONObject(reply).getJSONArray("accepted"));
            if (accepted.isEmpty()) break;
            db.questions().markSent(accepted);
            sent = true;
        }
        return sent;
    }

    /** Statuses, notes and answers from officers. Skipped until this phone has sent something. */
    private boolean fetchUpdates() throws IOException, JSONException {
        if (db.reports().sentCount() == 0 && db.questions().sentCount() == 0) return false;
        JSONObject reply = new JSONObject(http.get(baseUrl + "/api/updates", device()));
        int changed = 0;
        JSONArray reports = reply.optJSONArray("reports");
        if (reports != null) {
            for (int i = 0; i < reports.length(); i++) {
                JSONObject r = reports.getJSONObject(i);
                String verification = r.optString("verification", Report.Verification.REPORTED.name());
                if (!known(verification)) continue;
                changed += db.reports().applyOfficer(r.getLong("phoneId"), verification, text(r, "note"));
            }
        }
        JSONArray questions = reply.optJSONArray("questions");
        if (questions != null) {
            for (int i = 0; i < questions.length(); i++) {
                JSONObject q = questions.getJSONObject(i);
                String answer = text(q, "answer");
                if (answer == null) continue;
                changed += db.questions().applyAnswer(q.getLong("phoneId"), answer,
                        text(q, "answeredBy"), time(text(q, "answeredAt")));
            }
        }
        return changed > 0;
    }

    private static boolean known(String verification) {
        for (Report.Verification v : Report.Verification.values()) if (v.name().equals(verification)) return true;
        return false;
    }

    private static String text(JSONObject o, String key) {
        return o.isNull(key) ? null : o.optString(key, null);
    }

    private static long time(String iso) {
        if (iso == null) return System.currentTimeMillis();
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (DateTimeParseException e) {
            return System.currentTimeMillis();
        }
    }

    private static List<Long> ids(JSONArray a) throws JSONException {
        List<Long> out = new ArrayList<>(a.length());
        for (int i = 0; i < a.length(); i++) out.add(a.getLong(i));
        return out;
    }
}
