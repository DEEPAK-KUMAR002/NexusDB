package vectordb;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class BruteForce {
    public List<VectorItem> items = new ArrayList<>();

    public void insert(VectorItem v) {
        items.add(v);
    }

    public static class Hit implements Comparable<Hit> {
        public float dist;
        public int id;
        public Hit(float dist, int id) { this.dist = dist; this.id = id; }
        @Override
        public int compareTo(Hit o) {
            return Float.compare(this.dist, o.dist);
        }
    }

    public List<Hit> knn(float[] q, int k, Distance dist) {
        PriorityQueue<Hit> pq = new PriorityQueue<>(k + 1, (a, b) -> Float.compare(b.dist, a.dist));
        for (VectorItem v : items) {
            float d = dist.compute(q, v.emb);
            pq.offer(new Hit(d, v.id));
            if (pq.size() > k) {
                pq.poll();
            }
        }
        
        List<Hit> res = new ArrayList<>();
        while (!pq.isEmpty()) {
            res.add(pq.poll());
        }
        java.util.Collections.reverse(res); // Ascending order
        return res;
    }

    public void remove(int id) {
        items.removeIf(v -> v.id == id);
    }
}
