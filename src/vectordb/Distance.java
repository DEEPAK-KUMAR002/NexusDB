package vectordb;

public interface Distance {
    float compute(float[] a, float[] b);

    public static final Distance EUCLIDEAN = (a, b) -> {
        float s = 0;
        for (int i = 0; i < a.length; i++) {
            float d = a[i] - b[i];
            s += d * d;
        }
        return (float) Math.sqrt(s);
    };

    public static final Distance COSINE = (a, b) -> {
        float dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na < 1e-9f || nb < 1e-9f) return 1.0f;
        return 1.0f - (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    };

    public static final Distance MANHATTAN = (a, b) -> {
        float s = 0;
        for (int i = 0; i < a.length; i++) {
            s += Math.abs(a[i] - b[i]);
        }
        return s;
    };

    public static Distance get(String metric) {
        if ("cosine".equals(metric)) return COSINE;
        if ("manhattan".equals(metric)) return MANHATTAN;
        return EUCLIDEAN;
    }
}
