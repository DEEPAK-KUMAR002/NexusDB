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
        return item.id;
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
