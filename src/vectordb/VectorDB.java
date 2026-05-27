package vectordb;

import java.util.*;

public class VectorDB {
    private Map<Integer, VectorItem> store = new HashMap<>();
    private BruteForce bf = new BruteForce();
    private KDTree kdt;
    private HNSW hnsw = new HNSW(16, 200);
    private int nextId = 1;
    public final int dims;

    public VectorDB(int dims) {
        this.dims = dims;
        this.kdt = new KDTree(dims);
    }

    public synchronized int insert(String meta, String cat, float[] emb, Distance dist) {
        VectorItem v = new VectorItem(nextId++, meta, cat, emb);
        store.put(v.id, v);
        bf.insert(v);
        kdt.insert(v);
        hnsw.insert(v, dist);
        saveToFile();
        return v.id;
    }

    public synchronized void load(int id, String meta, String cat, float[] emb, Distance dist) {
        VectorItem v = new VectorItem(id, meta, cat, emb);
        store.put(id, v);
        bf.insert(v);
        kdt.insert(v);
        hnsw.insert(v, dist);
        if (id >= nextId) {
            nextId = id + 1;
        }
    }

    private static final String FILE_PATH = "data/vectordb.jsonl";

    public synchronized void saveToFile() {
        StringBuilder sb = new StringBuilder();
        for (VectorItem item : store.values()) {
            String line = "{\"id\":" + item.id + ","
                    + "\"metadata\":" + Main.jS(item.metadata) + ","
                    + "\"category\":" + Main.jS(item.category) + ","
                    + "\"emb\":" + Main.jVec(item.emb) + "}";
            sb.append(line).append("\n");
        }
        String content = sb.toString();

        String bucket = System.getenv("KVDB_BUCKET");
        if (bucket != null && !bucket.trim().isEmpty()) {
            String url = "https://kvdb.io/" + bucket.trim() + "/vectordb";
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
            String url = "https://kvdb.io/" + bucket.trim() + "/vectordb";
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
            String meta = GeminiClient.extractStr(line, "metadata");
            String cat = GeminiClient.extractStr(line, "category");
            float[] emb = parseVecField(line, "emb");
            if (id > 0 && !meta.isEmpty() && emb.length > 0) {
                load(id, meta, cat, emb, Distance.COSINE);
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

    public synchronized boolean remove(int id) {
        if (!store.containsKey(id)) return false;
        store.remove(id);
        bf.remove(id);
        hnsw.remove(id);
        List<VectorItem> rem = new ArrayList<>(store.values());
        kdt.rebuild(rem);
        saveToFile();
        return true;
    }

    public static class SearchOut {
        public List<Hit> hits = new ArrayList<>();
        public long us;
        public String algo, metric;
    }

    public static class Hit {
        public int id;
        public String meta, cat;
        public float[] emb;
        public float dist;
        public Hit(int id, String meta, String cat, float[] emb, float dist) {
            this.id = id; this.meta = meta; this.cat = cat; this.emb = emb; this.dist = dist;
        }
    }

    public synchronized SearchOut search(float[] q, int k, String metric, String algo) {
        Distance dfn = Distance.get(metric);
        long t0 = System.nanoTime();
        
        List<BruteForce.Hit> raw;
        if ("bruteforce".equals(algo)) raw = bf.knn(q, k, dfn);
        else if ("kdtree".equals(algo)) raw = kdt.knn(q, k, dfn);
        else raw = hnsw.knn(q, k, 50, dfn);
        
        long us = (System.nanoTime() - t0) / 1000;
        
        SearchOut out = new SearchOut();
        out.us = us;
        out.algo = algo;
        out.metric = metric;
        
        for (BruteForce.Hit h : raw) {
            if (store.containsKey(h.id)) {
                VectorItem v = store.get(h.id);
                out.hits.add(new Hit(v.id, v.metadata, v.category, v.emb, h.dist));
            }
        }
        return out;
    }

    public static class BenchOut {
        public long bfUs, kdUs, hnswUs;
        public int n;
    }

    public synchronized BenchOut benchmark(float[] q, int k, String metric) {
        Distance dfn = Distance.get(metric);
        BenchOut b = new BenchOut();
        
        long t0 = System.nanoTime();
        bf.knn(q, k, dfn);
        b.bfUs = (System.nanoTime() - t0) / 1000;
        
        t0 = System.nanoTime();
        kdt.knn(q, k, dfn);
        b.kdUs = (System.nanoTime() - t0) / 1000;
        
        t0 = System.nanoTime();
        hnsw.knn(q, k, 50, dfn);
        b.hnswUs = (System.nanoTime() - t0) / 1000;
        
        b.n = store.size();
        return b;
    }

    public synchronized List<VectorItem> all() {
        return new ArrayList<>(store.values());
    }

    public synchronized HNSW.GraphInfo hnswInfo() {
        return hnsw.getInfo();
    }

    public synchronized int size() {
        return store.size();
    }
}
