package vectordb;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    static final int DIMS = 16;

    static void loadDemo(VectorDB db) {
        Distance dist = Distance.get("cosine");
        db.insert("Linked List: nodes connected by pointers", "cs", new float[]{0.90f,0.85f,0.72f,0.68f,0.12f,0.08f,0.15f,0.10f,0.05f,0.08f,0.06f,0.09f,0.07f,0.11f,0.08f,0.06f}, dist);
        db.insert("Binary Search Tree: O(log n) search and insert", "cs", new float[]{0.88f,0.82f,0.78f,0.74f,0.15f,0.10f,0.08f,0.12f,0.06f,0.07f,0.08f,0.05f,0.09f,0.06f,0.07f,0.10f}, dist);
        db.insert("Dynamic Programming: memoization overlapping subproblems", "cs", new float[]{0.82f,0.76f,0.88f,0.80f,0.20f,0.18f,0.12f,0.09f,0.07f,0.06f,0.08f,0.07f,0.08f,0.09f,0.06f,0.07f}, dist);
        db.insert("Graph BFS and DFS: breadth and depth first traversal", "cs", new float[]{0.85f,0.80f,0.75f,0.82f,0.18f,0.14f,0.10f,0.08f,0.06f,0.09f,0.07f,0.06f,0.10f,0.08f,0.09f,0.07f}, dist);
        db.insert("Hash Table: O(1) lookup with collision chaining", "cs", new float[]{0.87f,0.78f,0.70f,0.76f,0.13f,0.11f,0.09f,0.14f,0.08f,0.07f,0.06f,0.08f,0.07f,0.10f,0.08f,0.09f}, dist);
        db.insert("Calculus: derivatives integrals and limits", "math", new float[]{0.12f,0.15f,0.18f,0.10f,0.91f,0.86f,0.78f,0.72f,0.08f,0.06f,0.07f,0.09f,0.07f,0.08f,0.06f,0.10f}, dist);
        db.insert("Linear Algebra: matrices eigenvalues eigenvectors", "math", new float[]{0.20f,0.18f,0.15f,0.12f,0.88f,0.90f,0.82f,0.76f,0.09f,0.07f,0.08f,0.06f,0.10f,0.07f,0.08f,0.09f}, dist);
        db.insert("Probability: distributions random variables Bayes theorem", "math", new float[]{0.15f,0.12f,0.20f,0.18f,0.84f,0.80f,0.88f,0.82f,0.07f,0.08f,0.06f,0.10f,0.09f,0.06f,0.09f,0.08f}, dist);
        db.insert("Number Theory: primes modular arithmetic RSA cryptography", "math", new float[]{0.22f,0.16f,0.14f,0.20f,0.80f,0.85f,0.76f,0.90f,0.08f,0.09f,0.07f,0.06f,0.08f,0.10f,0.07f,0.06f}, dist);
        db.insert("Combinatorics: permutations combinations generating functions", "math", new float[]{0.18f,0.20f,0.16f,0.14f,0.86f,0.78f,0.84f,0.80f,0.06f,0.07f,0.09f,0.08f,0.06f,0.09f,0.10f,0.07f}, dist);
        db.insert("Neapolitan Pizza: wood-fired dough San Marzano tomatoes", "food", new float[]{0.08f,0.06f,0.09f,0.07f,0.07f,0.08f,0.06f,0.09f,0.90f,0.86f,0.78f,0.72f,0.08f,0.06f,0.09f,0.07f}, dist);
        db.insert("Sushi: vinegared rice raw fish and nori rolls", "food", new float[]{0.06f,0.08f,0.07f,0.09f,0.09f,0.06f,0.08f,0.07f,0.86f,0.90f,0.82f,0.76f,0.07f,0.09f,0.06f,0.08f}, dist);
        db.insert("Ramen: noodle soup with chashu pork and soft-boiled eggs", "food", new float[]{0.09f,0.07f,0.06f,0.08f,0.08f,0.09f,0.07f,0.06f,0.82f,0.78f,0.90f,0.84f,0.09f,0.07f,0.08f,0.06f}, dist);
        db.insert("Tacos: corn tortillas with carnitas salsa and cilantro", "food", new float[]{0.07f,0.09f,0.08f,0.06f,0.06f,0.07f,0.09f,0.08f,0.78f,0.82f,0.86f,0.90f,0.06f,0.08f,0.07f,0.09f}, dist);
        db.insert("Croissant: laminated pastry with buttery flaky layers", "food", new float[]{0.06f,0.07f,0.10f,0.09f,0.10f,0.06f,0.07f,0.10f,0.85f,0.80f,0.76f,0.82f,0.09f,0.07f,0.10f,0.06f}, dist);
        db.insert("Basketball: fast-paced shooting dribbling slam dunks", "sports", new float[]{0.09f,0.07f,0.08f,0.10f,0.08f,0.09f,0.07f,0.06f,0.08f,0.07f,0.09f,0.06f,0.91f,0.85f,0.78f,0.72f}, dist);
        db.insert("Football: tackles touchdowns field goals and strategy", "sports", new float[]{0.07f,0.09f,0.06f,0.08f,0.09f,0.07f,0.10f,0.08f,0.07f,0.09f,0.08f,0.07f,0.87f,0.89f,0.82f,0.76f}, dist);
        db.insert("Tennis: racket volleys groundstrokes and Wimbledon serves", "sports", new float[]{0.08f,0.06f,0.09f,0.07f,0.07f,0.08f,0.06f,0.09f,0.09f,0.06f,0.07f,0.08f,0.83f,0.80f,0.88f,0.82f}, dist);
        db.insert("Chess: openings endgames tactics strategic board game", "sports", new float[]{0.25f,0.20f,0.22f,0.18f,0.22f,0.18f,0.20f,0.15f,0.06f,0.08f,0.07f,0.09f,0.80f,0.84f,0.78f,0.90f}, dist);
        db.insert("Swimming: butterfly freestyle backstroke Olympic competition", "sports", new float[]{0.06f,0.08f,0.07f,0.09f,0.08f,0.06f,0.09f,0.07f,0.10f,0.08f,0.06f,0.07f,0.85f,0.82f,0.86f,0.80f}, dist);
    }

    public static String jS(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    public static String jVec(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(Locale.US, "%.4f", v[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<String> chunkText(String text, int chunkWords, int overlapWords) {
        String[] words = text.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) return new ArrayList<>();
        if (words.length <= chunkWords) return Collections.singletonList(text);

        List<String> chunks = new ArrayList<>();
        int step = chunkWords - overlapWords;
        for (int i = 0; i < words.length; i += step) {
            int end = Math.min(i + chunkWords, words.length);
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < end; j++) {
                if (j > i) chunk.append(" ");
                chunk.append(words[j]);
            }
            chunks.add(chunk.toString());
            if (end == words.length) break;
        }
        return chunks;
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    private static float[] parseVec(String s) {
        if (s == null || s.isEmpty()) return new float[0];
        String[] parts = s.split(",");
        List<Float> list = new ArrayList<>();
        for (String p : parts) {
            try { list.add(Float.parseFloat(p)); } catch (Exception ignored) {}
        }
        float[] res = new float[list.size()];
        for (int i = 0; i < res.length; i++) res[i] = list.get(i);
        return res;
    }
    
    private static int extractInt(String body, String key, int def) {
        try {
            String val = OllamaClient.extractStr(body, key);
            if (val.isEmpty()) {
                // maybe it's not a string in json but a number
                int p = body.indexOf("\"" + key + "\"");
                if (p == -1) return def;
                p = body.indexOf(':', p) + 1;
                while (p < body.length() && (body.charAt(p) == ' ' || body.charAt(p) == '\t')) p++;
                int end = p;
                while (end < body.length() && Character.isDigit(body.charAt(end))) end++;
                return Integer.parseInt(body.substring(p, end));
            }
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }
    }

    private static void cors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        cors(exchange);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             Scanner s = new Scanner(is, StandardCharsets.UTF_8.name())) {
            return s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
    }

    public static void main(String[] args) throws Exception {
        VectorDB db = new VectorDB(DIMS);
        DocumentDB docDB = new DocumentDB();
        OllamaClient ollama = new OllamaClient();

        loadDemo(db);

        boolean ollamaUp = ollama.isAvailable();
        System.out.println("=== VectorDB Engine (Java) ===");
        System.out.println("http://localhost:8080");
        System.out.println(db.size() + " demo vectors | " + DIMS + " dims | HNSW+KD-Tree+BruteForce");
        System.out.println("Ollama: " + (ollamaUp ? "ONLINE" : "OFFLINE (install from ollama.com)"));
        if (ollamaUp) {
            System.out.println("  embed model: " + ollama.embedModel + "  gen model: " + ollama.genModel);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                cors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            String path = exchange.getRequestURI().getPath();
            
            if ("/".equals(path) || "/index.html".equals(path)) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get("index.html")), StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html");
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (IOException e) {
                    exchange.sendResponseHeaders(404, -1);
                }
                return;
            }
            
            if ("/search".equals(path)) {
                Map<String, String> q = queryToMap(exchange.getRequestURI().getQuery());
                float[] v = parseVec(q.get("v"));
                if (v.length != DIMS) {
                    sendResponse(exchange, 400, "{\"error\":\"need " + DIMS + "D vector\"}");
                    return;
                }
                int k = 5;
                try { k = Integer.parseInt(q.get("k")); } catch (Exception ignored) {}
                String metric = q.getOrDefault("metric", "cosine");
                String algo = q.getOrDefault("algo", "hnsw");
                
                VectorDB.SearchOut out = db.search(v, k, metric, algo);
                StringBuilder sb = new StringBuilder();
                sb.append("{\"results\":[");
                for (int i = 0; i < out.hits.size(); i++) {
                    if (i > 0) sb.append(",");
                    VectorDB.Hit h = out.hits.get(i);
                    sb.append("{\"id\":").append(h.id)
                      .append(",\"metadata\":").append(jS(h.meta))
                      .append(",\"category\":").append(jS(h.cat))
                      .append(",\"distance\":").append(String.format(Locale.US, "%.6f", h.dist))
                      .append(",\"embedding\":").append(jVec(h.emb)).append("}");
                }
                sb.append("],\"latencyUs\":").append(out.us)
                  .append(",\"algo\":").append(jS(out.algo))
                  .append(",\"metric\":").append(jS(out.metric)).append("}");
                
                sendResponse(exchange, 200, sb.toString());
                return;
            }
            
            if ("/insert".equals(path) && "POST".equals(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                String meta = OllamaClient.extractStr(body, "metadata");
                String cat = OllamaClient.extractStr(body, "category");
                
                // extract embedding array
                float[] emb = new float[0];
                int p = body.indexOf("\"embedding\"");
                if (p != -1) {
                    p = body.indexOf('[', p);
                    if (p != -1) {
                        int e = body.indexOf(']', p);
                        if (e != -1) {
                            emb = parseVec(body.substring(p + 1, e));
                        }
                    }
                }
                
                if (meta.isEmpty() || emb.length != DIMS) {
                    sendResponse(exchange, 400, "{\"error\":\"invalid body\"}");
                    return;
                }
                int id = db.insert(meta, cat, emb, Distance.COSINE);
                sendResponse(exchange, 200, "{\"id\":" + id + "}");
                return;
            }
            
            if (path.startsWith("/delete/") && "DELETE".equals(exchange.getRequestMethod())) {
                try {
                    int id = Integer.parseInt(path.substring("/delete/".length()));
                    boolean ok = db.remove(id);
                    sendResponse(exchange, 200, "{\"ok\":" + (ok ? "true" : "false") + "}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"invalid id\"}");
                }
                return;
            }
            
            if ("/items".equals(path)) {
                List<VectorItem> items = db.all();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) sb.append(",");
                    VectorItem v = items.get(i);
                    sb.append("{\"id\":").append(v.id)
                      .append(",\"metadata\":").append(jS(v.metadata))
                      .append(",\"category\":").append(jS(v.category))
                      .append(",\"embedding\":").append(jVec(v.emb)).append("}");
                }
                sb.append("]");
                sendResponse(exchange, 200, sb.toString());
                return;
            }
            
            if ("/benchmark".equals(path)) {
                Map<String, String> q = queryToMap(exchange.getRequestURI().getQuery());
                float[] v = parseVec(q.get("v"));
                if (v.length != DIMS) {
                    sendResponse(exchange, 400, "{\"error\":\"need " + DIMS + "D vector\"}");
                    return;
                }
                int k = 5;
                try { k = Integer.parseInt(q.get("k")); } catch (Exception ignored) {}
                String metric = q.getOrDefault("metric", "cosine");
                
                VectorDB.BenchOut b = db.benchmark(v, k, metric);
                String res = "{\"bruteforceUs\":" + b.bfUs + ",\"kdtreeUs\":" + b.kdUs
                           + ",\"hnswUs\":" + b.hnswUs + ",\"itemCount\":" + b.n + "}";
                sendResponse(exchange, 200, res);
                return;
            }
            
            if ("/hnsw-info".equals(path)) {
                HNSW.GraphInfo gi = db.hnswInfo();
                StringBuilder sb = new StringBuilder();
                sb.append("{\"topLayer\":").append(gi.topLayer)
                  .append(",\"nodeCount\":").append(gi.nodeCount)
                  .append(",\"nodesPerLayer\":[");
                for (int i = 0; i < gi.nodesPerLayer.size(); i++) {
                    if (i > 0) sb.append(","); sb.append(gi.nodesPerLayer.get(i));
                }
                sb.append("],\"edgesPerLayer\":[");
                for (int i = 0; i < gi.edgesPerLayer.size(); i++) {
                    if (i > 0) sb.append(","); sb.append(gi.edgesPerLayer.get(i));
                }
                sb.append("],\"nodes\":[");
                for (int i = 0; i < gi.nodes.size(); i++) {
                    if (i > 0) sb.append(",");
                    HNSW.GraphInfo.NV n = gi.nodes.get(i);
                    sb.append("{\"id\":").append(n.id)
                      .append(",\"metadata\":").append(jS(n.metadata))
                      .append(",\"category\":").append(jS(n.category))
                      .append(",\"maxLyr\":").append(n.maxLyr).append("}");
                }
                sb.append("],\"edges\":[");
                for (int i = 0; i < gi.edges.size(); i++) {
                    if (i > 0) sb.append(",");
                    HNSW.GraphInfo.EV e = gi.edges.get(i);
                    sb.append("{\"src\":").append(e.src)
                      .append(",\"dst\":").append(e.dst)
                      .append(",\"lyr\":").append(e.lyr).append("}");
                }
                sb.append("]}");
                sendResponse(exchange, 200, sb.toString());
                return;
            }
            
            // DOC + RAG ENDPOINTS
            
            if ("/doc/insert".equals(path) && "POST".equals(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                String title = OllamaClient.extractStr(body, "title");
                String text = OllamaClient.extractStr(body, "text");
                if (title.isEmpty() || text.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"need title and text\"}");
                    return;
                }
                List<String> chunks = chunkText(text, 250, 30);
                List<Integer> ids = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    float[] emb = ollama.embed(chunks.get(i));
                    if (emb.length == 0) {
                        sendResponse(exchange, 500, "{\"error\":\"Ollama unavailable. Install from https://ollama.com\"}");
                        return;
                    }
                    String chunkTitle = chunks.size() > 1 ? title + " [" + (i+1) + "/" + chunks.size() + "]" : title;
                    ids.add(docDB.insert(chunkTitle, chunks.get(i), emb));
                }
                StringBuilder sb = new StringBuilder("{\"ids\":[");
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) sb.append(","); sb.append(ids.get(i));
                }
                sb.append("],\"chunks\":").append(chunks.size())
                  .append(",\"dims\":").append(docDB.getDims()).append("}");
                sendResponse(exchange, 200, sb.toString());
                return;
            }
            
            if (path.startsWith("/doc/delete/") && "DELETE".equals(exchange.getRequestMethod())) {
                try {
                    int id = Integer.parseInt(path.substring("/doc/delete/".length()));
                    boolean ok = docDB.remove(id);
                    sendResponse(exchange, 200, "{\"ok\":" + (ok ? "true" : "false") + "}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"invalid id\"}");
                }
                return;
            }
            
            if ("/doc/list".equals(path)) {
                List<DocItem> docs = docDB.all();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < docs.size(); i++) {
                    if (i > 0) sb.append(",");
                    DocItem d = docs.get(i);
                    String preview = d.text.length() > 120 ? d.text.substring(0, 120) + "..." : d.text;
                    int words = d.text.trim().split("\\s+").length;
                    sb.append("{\"id\":").append(d.id)
                      .append(",\"title\":").append(jS(d.title))
                      .append(",\"preview\":").append(jS(preview))
                      .append(",\"words\":").append(words).append("}");
                }
                sb.append("]");
                sendResponse(exchange, 200, sb.toString());
                return;
            }
            
            if ("/doc/ask".equals(path) && "POST".equals(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                String question = OllamaClient.extractStr(body, "question");
                int k = extractInt(body, "k", 3);
                if (question.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"need question\"}");
                    return;
                }
                
                float[] qEmb = ollama.embed(question);
                if (qEmb.length == 0) {
                    sendResponse(exchange, 500, "{\"error\":\"Ollama unavailable\"}");
                    return;
                }
                
                List<DocumentDB.Hit> hits = docDB.search(qEmb, k);
                
                StringBuilder ctx = new StringBuilder();
                for (int i = 0; i < hits.size(); i++) {
                    ctx.append("[").append(i+1).append("] ").append(hits.get(i).item.title).append(":\n")
                       .append(hits.get(i).item.text).append("\n\n");
                }
                
                String prompt = "You are a helpful assistant. Answer the user's question directly. "
                              + "Use the provided context if it contains relevant information. "
                              + "If it doesn't, just use your own general knowledge. "
                              + "IMPORTANT: Do NOT mention the 'context', 'provided text', or say things like 'the context doesn't mention'. "
                              + "Just answer the question naturally.\n\n"
                              + "Context:\n" + ctx.toString()
                              + "Question: " + question + "\n\nAnswer:";
                              
                String answer = ollama.generate(prompt);
                
                StringBuilder sb = new StringBuilder();
                sb.append("{\"answer\":").append(jS(answer))
                  .append(",\"model\":").append(jS(ollama.genModel))
                  .append(",\"contexts\":[");
                for (int i = 0; i < hits.size(); i++) {
                    if (i > 0) sb.append(",");
                    DocumentDB.Hit h = hits.get(i);
                    sb.append("{\"id\":").append(h.item.id)
                      .append(",\"title\":").append(jS(h.item.title))
                      .append(",\"text\":").append(jS(h.item.text))
                      .append(",\"distance\":").append(String.format(Locale.US, "%.4f", h.dist)).append("}");
                }
                sb.append("],\"docCount\":").append(docDB.size()).append("}");
                sendResponse(exchange, 200, sb.toString());
                return;
            }
            
            if ("/status".equals(path)) {
                boolean up = ollama.isAvailable();
                String res = "{\"ollamaAvailable\":" + (up ? "true" : "false")
                           + ",\"embedModel\":" + jS(ollama.embedModel)
                           + ",\"genModel\":" + jS(ollama.genModel)
                           + ",\"docCount\":" + docDB.size()
                           + ",\"docDims\":" + docDB.getDims()
                           + ",\"demoDims\":" + DIMS
                           + ",\"demoCount\":" + db.size() + "}";
                sendResponse(exchange, 200, res);
                return;
            }
            
            if ("/stats".equals(path)) {
                String res = "{\"count\":" + db.size()
                           + ",\"dims\":" + DIMS
                           + ",\"algorithms\":[\"bruteforce\",\"kdtree\",\"hnsw\"]"
                           + ",\"metrics\":[\"euclidean\",\"cosine\",\"manhattan\"]}";
                sendResponse(exchange, 200, res);
                return;
            }
            
            exchange.sendResponseHeaders(404, -1);
        });

        server.setExecutor(null);
        server.start();
    }
}
