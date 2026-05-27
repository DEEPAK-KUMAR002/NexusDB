package vectordb;

import java.util.*;

public class DocumentDB {
    private Map<Integer, DocItem> store = new HashMap<>();
    private HNSW hnsw = new HNSW(16, 200);
    private BruteForce bf = new BruteForce(); // fallback for small sets
    private int nextId = 1;
    private int dims = 0;

    public synchronized int insert(String title, String text, float[] emb) {
        if (dims == 0) dims = emb.length;
        DocItem item = new DocItem(nextId++, title, text, emb);
        store.put(item.id, item);
        VectorItem vi = new VectorItem(item.id, title, "doc", emb);
        hnsw.insert(vi, Distance.COSINE);
        bf.insert(vi);
        saveToFile();
        return item.id;
    }

    public synchronized void load(int id, String title, String text, float[] emb) {
        if (dims == 0) dims = emb.length;
        DocItem item = new DocItem(id, title, text, emb);
        store.put(id, item);
        VectorItem vi = new VectorItem(id, title, "doc", emb);
        hnsw.insert(vi, Distance.COSINE);
        bf.insert(vi);
        if (id >= nextId) {
            nextId = id + 1;
        }
    }

    private static final String FILE_PATH = "data/documentdb.jsonl";

    public synchronized void saveToFile() {
        StringBuilder sb = new StringBuilder();
        for (DocItem item : store.values()) {
            String line = "{\"id\":" + item.id + ","
                    + "\"title\":" + Main.jS(item.title) + ","
                    + "\"text\":" + Main.jS(item.text) + ","
                    + "\"emb\":" + Main.jVec(item.emb) + "}";
            sb.append(line).append("\n");
        }
        String content = sb.toString();

        String bucket = System.getenv("KVDB_BUCKET");
        if (bucket != null && !bucket.trim().isEmpty()) {
            String url = "https://kvdb.io/" + bucket.trim() + "/documentdb";
            CloudStorage.put(url, content);
        }

        try {
            java.io.File file = new java.io.File(FILE_PATH);
            java.io.File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)))) {
                out.print(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadFromFile() {
        String bucket = System.getenv("KVDB_BUCKET");
        String content = "";
        if (bucket != null && !bucket.trim().isEmpty()) {
            String url = "https://kvdb.io/" + bucket.trim() + "/documentdb";
            content = CloudStorage.get(url);
        }

        if (content == null || content.isEmpty()) {
            java.io.File file = new java.io.File(FILE_PATH);
            if (file.exists()) {
                try {
                    content = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (content == null || content.isEmpty()) return;

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int id = extractInt(line, "id", 0);
            String title = GeminiClient.extractStr(line, "title");
            String text = GeminiClient.extractStr(line, "text");
            float[] emb = parseVecField(line, "emb");
            if (id > 0 && !title.isEmpty() && emb.length > 0) {
                load(id, title, text, emb);
            }
        }
    }

    private static int extractInt(String body, String key, int def) {
        try {
            String val = GeminiClient.extractStr(body, key);
            if (val.isEmpty()) {
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

    private static float[] parseVecField(String body, String key) {
        int p = body.indexOf("\"" + key + "\"");
        if (p == -1) return new float[0];
        p = body.indexOf('[', p);
        if (p == -1) return new float[0];
        int e = body.indexOf(']', p);
        if (e == -1) return new float[0];
        String s = body.substring(p + 1, e);
        if (s.isEmpty()) return new float[0];
        String[] parts = s.split(",");
        List<Float> list = new ArrayList<>();
        for (String part : parts) {
            try { list.add(Float.parseFloat(part.trim())); } catch (Exception ignored) {}
        }
        float[] res = new float[list.size()];
        for (int i = 0; i < res.length; i++) res[i] = list.get(i);
        return res;
    }

    public static class Hit {
        public float dist;
        public DocItem item;
        public Hit(float dist, DocItem item) { this.dist = dist; this.item = item; }
    }

    public synchronized List<Hit> search(float[] q, int k) {
        return search(q, k, 0.7f);
    }

    public synchronized List<Hit> search(float[] q, int k, float max_dist) {
        if (store.isEmpty()) return new ArrayList<>();
        List<BruteForce.Hit> raw = (store.size() < 10) 
            ? bf.knn(q, k, Distance.COSINE)
            : hnsw.knn(q, k, 50, Distance.COSINE);
        
        List<Hit> out = new ArrayList<>();
        for (BruteForce.Hit h : raw) {
            if (store.containsKey(h.id) && h.dist <= max_dist) {
                out.add(new Hit(h.dist, store.get(h.id)));
            }
        }
        return out;
    }

    public synchronized boolean remove(int id) {
        if (!store.containsKey(id)) return false;
        store.remove(id);
        hnsw.remove(id);
        bf.remove(id);
        saveToFile();
        return true;
    }

    public synchronized List<DocItem> all() {
        return new ArrayList<>(store.values());
    }

    public synchronized int size() {
        return store.size();
    }

    public synchronized int getDims() {
        return dims;
    }
}
