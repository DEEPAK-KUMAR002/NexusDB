package vectordb;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GeminiClient {
    private String apiKey;
    private HttpClient client;

    public GeminiClient() {
        this.apiKey = System.getenv("GEMINI_API_KEY");
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
        }
        return sb.toString();
    }

    private List<Float> parseEmbedding(String body) {
        int p = body.indexOf("\"values\"");
        if (p == -1) return new ArrayList<>();
        p = body.indexOf('[', p);
        if (p == -1) return new ArrayList<>();
        int e = p + 1;
        int depth = 1;
        while (e < body.length() && depth > 0) {
            if (body.charAt(e) == '[') depth++;
            else if (body.charAt(e) == ']') depth--;
            e++;
        }
        String arrStr = body.substring(p + 1, e - 1);
        List<Float> res = new ArrayList<>();
        for (String token : arrStr.split(",")) {
            try {
                res.add(Float.parseFloat(token.trim()));
            } catch (Exception ignored) {}
        }
        return res;
    }

    public static String extractStr(String body, String key) {
        int p = body.indexOf("\"" + key + "\"");
        if (p == -1) return "";
        p = body.indexOf(':', p) + 1;
        while (p < body.length() && (body.charAt(p) == ' ' || body.charAt(p) == '\t')) p++;
        if (p >= body.length() || body.charAt(p) != '"') return "";
        p++;
        StringBuilder res = new StringBuilder();
        while (p < body.length()) {
            if (body.charAt(p) == '"') {
                int count = 0;
                int tmp = p - 1;
                while (tmp >= 0 && body.charAt(tmp) == '\\') { count++; tmp--; }
                if (count % 2 == 0) break;
            }
            if (body.charAt(p) == '\\' && p + 1 < body.length()) {
                p++;
                switch (body.charAt(p)) {
                    case '"': res.append('"'); break;
                    case '\\': res.append('\\'); break;
                    case 'n': res.append('\n'); break;
                    case 'r': res.append('\r'); break;
                    case 't': res.append('\t'); break;
                    default: res.append(body.charAt(p)); break;
                }
            } else {
                res.append(body.charAt(p));
            }
            p++;
        }
        return res.toString();
    }

    private String parseResponse(String body) {
        int p = body.indexOf("\"text\"");
        if (p == -1) return "ERROR: Failed to parse Gemini response.";
        p = body.indexOf(':', p) + 1;
        while (p < body.length() && (body.charAt(p) == ' ' || body.charAt(p) == '\t')) p++;
        if (p >= body.length() || body.charAt(p) != '"') return "";
        p++;
        StringBuilder res = new StringBuilder();
        while (p < body.length()) {
            if (body.charAt(p) == '"') {
                // handle escaped quotes
                int count = 0;
                int tmp = p - 1;
                while (tmp >= 0 && body.charAt(tmp) == '\\') {
                    count++;
                    tmp--;
                }
                if (count % 2 == 0) break; // unescaped quote ends the string
            }
            if (body.charAt(p) == '\\' && p + 1 < body.length()) {
                p++;
                switch (body.charAt(p)) {
                    case '"': res.append('"'); break;
                    case '\\': res.append('\\'); break;
                    case 'n': res.append('\n'); break;
                    case 'r': res.append('\r'); break;
                    case 't': res.append('\t'); break;
                    default: res.append(body.charAt(p)); break;
                }
            } else {
                res.append(body.charAt(p));
            }
            p++;
        }
        return res.toString();
    }

    public boolean isAvailable() {
        return this.apiKey != null && !this.apiKey.trim().isEmpty();
    }

    public float[] embed(String text) {
        if (!isAvailable()) return new float[0];
        try {
            String body = "{\"model\": \"models/gemini-embedding-001\", \"content\": {\"parts\": [{\"text\": \"" + esc(text) + "\"}]}}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("Gemini Embed Error: " + response.body());
                return new float[0];
            }
            List<Float> list = parseEmbedding(response.body());
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            return arr;
        } catch (Exception e) {
            e.printStackTrace();
            return new float[0];
        }
    }

    public String generate(String prompt) {
        if (!isAvailable()) return "ERROR: GEMINI_API_KEY environment variable is not set.";
        try {
            String body = "{\"contents\": [{\"parts\":[{\"text\": \"" + esc(prompt) + "\"}]}]}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "ERROR: Gemini API returned status " + response.statusCode() + " - " + response.body();
            }
            return parseResponse(response.body());
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: Exception while calling Gemini API: " + e.getMessage();
        }
    }
}
