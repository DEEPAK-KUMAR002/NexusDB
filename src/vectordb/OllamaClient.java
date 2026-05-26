package vectordb;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OllamaClient {
    private String host;
    private int port;
    private HttpClient client;

    public String embedModel = "nomic-embed-text";
    public String genModel = "llama3.2";

    public OllamaClient() {
        this("127.0.0.1", 11434);
    }

    public OllamaClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
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
        int p = body.indexOf("\"embedding\"");
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

    private String parseResponse(String body) {
        return extractStr(body, "response");
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
            if (body.charAt(p) == '"') break;
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
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + ":" + port + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public float[] embed(String text) {
        try {
            String body = "{\"model\":\"" + embedModel + "\",\"prompt\":\"" + esc(text) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + ":" + port + "/api/embeddings"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return new float[0];
            List<Float> list = parseEmbedding(response.body());
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            return arr;
        } catch (Exception e) {
            return new float[0];
        }
    }

    public String generate(String prompt) {
        try {
            String body = "{\"model\":\"" + genModel + "\","
                    + "\"prompt\":\"" + esc(prompt) + "\","
                    + "\"stream\":false}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + ":" + port + "/api/generate"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return "ERROR: Ollama unavailable. Run: ollama serve";
            return parseResponse(response.body());
        } catch (Exception e) {
            return "ERROR: Ollama unavailable. Run: ollama serve";
        }
    }
}
