package com.mixesgates.devplant.gates.gm_http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Small JSON requests to the DevPlant cloud. Throws IOException for no signal or a non-2xx answer. */
public final class JsonHttp {

    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    public String get(String url, Map<String, String> headers) throws IOException {
        return send("GET", url, null, headers);
    }

    public String post(String url, String json, Map<String, String> headers) throws IOException {
        return send("POST", url, json, headers);
    }

    private static String send(String method, String url, String json, Map<String, String> headers) throws IOException {
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        http.setConnectTimeout(CONNECT_TIMEOUT_MS);
        http.setReadTimeout(READ_TIMEOUT_MS);
        http.setRequestMethod(method);
        http.setRequestProperty("Accept", "application/json");
        for (Map.Entry<String, String> h : headers.entrySet()) http.setRequestProperty(h.getKey(), h.getValue());
        try {
            if (json != null) {
                byte[] body = json.getBytes(StandardCharsets.UTF_8);
                http.setDoOutput(true);
                http.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                http.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = http.getOutputStream()) {
                    out.write(body);
                }
            }
            int code = http.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " for " + url);
            try (InputStream in = http.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                return out.toString("UTF-8");
            }
        } finally {
            http.disconnect();
        }
    }
}
