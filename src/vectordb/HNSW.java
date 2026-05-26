package vectordb;

import java.util.*;

public class HNSW {
    private static class Node {
        VectorItem item;
        int maxLyr;
        List<List<Integer>> nbrs;
        Node(VectorItem item, int maxLyr) {
            this.item = item;
            this.maxLyr = maxLyr;
            this.nbrs = new ArrayList<>();
            for (int i = 0; i <= maxLyr; i++) {
                this.nbrs.add(new ArrayList<>());
            }
        }
    }

    private Map<Integer, Node> G = new HashMap<>();
    private int M, M0, ef_build;
    private float mL;
    private int topLayer = -1;
    private int entryPt = -1;
    private Random rng = new Random(42);

    public HNSW(int m, int efBuild) {
        this.M = m;
        this.M0 = 2 * m;
        this.ef_build = efBuild;
        this.mL = (float) (1.0 / Math.log(m));
    }

    private int randLevel() {
        return (int) Math.floor(-Math.log(rng.nextDouble()) * mL);
    }

    private List<BruteForce.Hit> searchLayer(float[] q, int ep, int ef, int lyr, Distance dist) {
        Set<Integer> vis = new HashSet<>();
        PriorityQueue<BruteForce.Hit> cands = new PriorityQueue<>(Comparator.comparingDouble(h -> h.dist)); // Min-heap
        PriorityQueue<BruteForce.Hit> found = new PriorityQueue<>((a, b) -> Float.compare(b.dist, a.dist)); // Max-heap

        float d0 = dist.compute(q, G.get(ep).item.emb);
        vis.add(ep);
        cands.offer(new BruteForce.Hit(d0, ep));
        found.offer(new BruteForce.Hit(d0, ep));

        while (!cands.isEmpty()) {
            BruteForce.Hit curr = cands.poll();
            if (found.size() >= ef && curr.dist > found.peek().dist) break;
            
            Node cNode = G.get(curr.id);
            if (lyr >= cNode.nbrs.size()) continue;
            
            for (int nid : cNode.nbrs.get(lyr)) {
                if (vis.contains(nid) || !G.containsKey(nid)) continue;
                vis.add(nid);
                float nd = dist.compute(q, G.get(nid).item.emb);
                if (found.size() < ef || nd < found.peek().dist) {
                    cands.offer(new BruteForce.Hit(nd, nid));
                    found.offer(new BruteForce.Hit(nd, nid));
                    if (found.size() > ef) found.poll();
                }
            }
        }

        List<BruteForce.Hit> res = new ArrayList<>();
        while (!found.isEmpty()) res.add(found.poll());
        Collections.reverse(res); // Sort ascending
        return res;
    }

    private List<Integer> selectNbrs(List<BruteForce.Hit> cands, int maxM) {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i < Math.min(cands.size(), maxM); i++) {
            r.add(cands.get(i).id);
        }
        return r;
    }

    public void insert(VectorItem item, Distance dist) {
        int id = item.id;
        int lvl = randLevel();
        Node newNode = new Node(item, lvl);
        G.put(id, newNode);

        if (entryPt == -1) {
            entryPt = id;
            topLayer = lvl;
            return;
        }

        int ep = entryPt;
        for (int lc = topLayer; lc > lvl; lc--) {
            if (lc < G.get(ep).nbrs.size()) {
                List<BruteForce.Hit> W = searchLayer(item.emb, ep, 1, lc, dist);
                if (!W.isEmpty()) ep = W.get(0).id;
            }
        }

        for (int lc = Math.min(topLayer, lvl); lc >= 0; lc--) {
            List<BruteForce.Hit> W = searchLayer(item.emb, ep, ef_build, lc, dist);
            int maxM = (lc == 0) ? M0 : M;
            List<Integer> sel = selectNbrs(W, maxM);
            newNode.nbrs.set(lc, new ArrayList<>(sel));

            for (int nid : sel) {
                if (!G.containsKey(nid)) continue;
                Node nNode = G.get(nid);
                if (nNode.nbrs.size() <= lc) {
                    while (nNode.nbrs.size() <= lc) nNode.nbrs.add(new ArrayList<>());
                }
                List<Integer> conn = nNode.nbrs.get(lc);
                conn.add(id);
                if (conn.size() > maxM) {
                    List<BruteForce.Hit> ds = new ArrayList<>();
                    for (int c : conn) {
                        if (G.containsKey(c)) {
                            ds.add(new BruteForce.Hit(dist.compute(nNode.item.emb, G.get(c).item.emb), c));
                        }
                    }
                    Collections.sort(ds);
                    conn.clear();
                    for (int i = 0; i < maxM && i < ds.size(); i++) {
                        conn.add(ds.get(i).id);
                    }
                }
            }
            if (!W.isEmpty()) ep = W.get(0).id;
        }

        if (lvl > topLayer) {
            topLayer = lvl;
            entryPt = id;
        }
    }

    public List<BruteForce.Hit> knn(float[] q, int k, int ef, Distance dist) {
        if (entryPt == -1) return new ArrayList<>();
        int ep = entryPt;
        for (int lc = topLayer; lc > 0; lc--) {
            if (lc < G.get(ep).nbrs.size()) {
                List<BruteForce.Hit> W = searchLayer(q, ep, 1, lc, dist);
                if (!W.isEmpty()) ep = W.get(0).id;
            }
        }
        List<BruteForce.Hit> W = searchLayer(q, ep, Math.max(ef, k), 0, dist);
        if (W.size() > k) W = W.subList(0, k);
        return W;
    }

    public void remove(int id) {
        if (!G.containsKey(id)) return;
        for (Node nd : G.values()) {
            for (List<Integer> layer : nd.nbrs) {
                layer.remove((Integer) id);
            }
        }
        if (entryPt == id) {
            entryPt = -1;
            for (int nid : G.keySet()) {
                if (nid != id) {
                    entryPt = nid;
                    break;
                }
            }
        }
        G.remove(id);
    }

    public static class GraphInfo {
        public int topLayer, nodeCount;
        public List<Integer> nodesPerLayer = new ArrayList<>();
        public List<Integer> edgesPerLayer = new ArrayList<>();
        public static class NV { public int id; public String metadata, category; public int maxLyr; }
        public static class EV { public int src, dst, lyr; }
        public List<NV> nodes = new ArrayList<>();
        public List<EV> edges = new ArrayList<>();
    }

    public GraphInfo getInfo() {
        GraphInfo gi = new GraphInfo();
        gi.topLayer = topLayer;
        gi.nodeCount = G.size();
        int maxL = Math.max(topLayer + 1, 1);
        for (int i = 0; i < maxL; i++) {
            gi.nodesPerLayer.add(0);
            gi.edgesPerLayer.add(0);
        }
        for (Map.Entry<Integer, Node> entry : G.entrySet()) {
            int id = entry.getKey();
            Node nd = entry.getValue();
            GraphInfo.NV nv = new GraphInfo.NV();
            nv.id = id; nv.metadata = nd.item.metadata; nv.category = nd.item.category; nv.maxLyr = nd.maxLyr;
            gi.nodes.add(nv);
            for (int lc = 0; lc <= nd.maxLyr && lc < maxL; lc++) {
                gi.nodesPerLayer.set(lc, gi.nodesPerLayer.get(lc) + 1);
                if (lc < nd.nbrs.size()) {
                    for (int nid : nd.nbrs.get(lc)) {
                        if (id < nid) {
                            gi.edgesPerLayer.set(lc, gi.edgesPerLayer.get(lc) + 1);
                            GraphInfo.EV ev = new GraphInfo.EV();
                            ev.src = id; ev.dst = nid; ev.lyr = lc;
                            gi.edges.add(ev);
                        }
                    }
                }
            }
        }
        return gi;
    }

    public int size() {
        return G.size();
    }
}
