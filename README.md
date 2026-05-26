# NexusDB — Build a Vector Database from Scratch in Java

A fully working **Vector Database** built from scratch in Java with a beautiful glassmorphic web UI.  
Implements **HNSW**, **KD-Tree**, and **Brute Force** search algorithms side-by-side, plus a **RAG pipeline** powered by the **Google Gemini API**.

> Built as an educational project to show how production vector databases like Pinecone, Weaviate, and Chroma actually work under the hood, perfectly integrated with real-world cloud AI.

---

## What This Project Does

| Feature | Description |
|---|---|
| **3 Search Algorithms** | HNSW (production-grade), KD-Tree, Brute Force — run all three and compare speed |
| **3 Distance Metrics** | Cosine similarity, Euclidean distance, Manhattan distance |
| **16D Demo Vectors** | 20 pre-loaded semantic vectors across 4 categories (CS, Math, Food, Sports) |
| **2D PCA Scatter Plot** | Live visualization of semantic space — watch clusters form |
| **Real Document Embedding** | Paste any text → The Gemini API embeds it with `text-embedding-004` (768D) |
| **RAG Pipeline** | Ask questions about your documents → HNSW retrieves context → Gemini answers |
| **Full REST API** | CRUD endpoints: insert, delete, search, benchmark, hnsw-info |

---

## How It Works

```
Your Text
    │
    ▼
Gemini API (text-embedding-004)    ← converts text to a 768-dimensional vector
    │
    ▼
HNSW Index (Java)                  ← indexes the vector in a multilayer graph
    │
    ▼
Semantic Search                    ← finds nearest neighbors in vector space
    │
    ▼
Gemini API (gemini-1.5-flash)      ← reads retrieved chunks, generates an answer
    │
    ▼
Answer
```

**HNSW (Hierarchical Navigable Small World)** is the same algorithm used by Pinecone, Weaviate, Chroma, and Milvus. It builds a multilayer graph where each layer is progressively sparser — searches start at the top layer and zoom in, achieving O(log N) complexity instead of O(N) for brute force.

---

## Prerequisites

You need **3 things** to run this project:

1. **Java JDK 11+** (gives you `javac` compiler and `java` runner)
2. **Git**
3. **Gemini API Key** (Free from Google AI Studio)

---

## Step-by-Step Setup

### Step 1 — Get a Free Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Sign in with your Google account and click **Create API Key**.
3. Copy the key.

### Step 2 — Install Java & Git

- Download and install a Java JDK (version 11 or higher) from Oracle or Adoptium.
- Download Git from https://git-scm.com/download/win

### Step 3 — Clone the Repository

Open **PowerShell** and run:

```powershell
git clone https://github.com/DEEPAK-KUMAR002/NexusDB.git
cd NexusDB
```

### Step 4 — Compile the Java Server

Inside the `NexusDB` folder, run:

```powershell
javac src/vectordb/*.java
```

### Step 5 — Run Everything

Before starting the server, set your Gemini API key as an environment variable:

```powershell
$env:GEMINI_API_KEY="your_api_key_here"
```

Start the VectorDB server:
```powershell
java -cp src vectordb.Main
```

You should see:
```
=== VectorDB Engine (Java) ===
http://localhost:8080
20 demo vectors | 16 dims | HNSW+KD-Tree+BruteForce
Gemini API: ONLINE
```

**Open your browser** and go to:
```
http://localhost:8080
```

---

## Using the Application

### Tab 1: Search (Demo Vectors)

- Type any concept in the search box: `binary tree`, `sushi`, `basketball`, `calculus`
- Choose your algorithm: **HNSW**, **KD-Tree**, or **Brute Force**
- Choose distance metric: **Cosine**, **Euclidean**, or **Manhattan**
- Click **⚡ SEARCH** — results appear with distances, the matching point glows on the scatter plot
- Click **▶ COMPARE ALL ALGOS** to run all 3 algorithms and compare their speed

**The scatter plot** shows all 20 vectors projected to 2D using PCA. Notice how the 4 semantic categories (CS, Math, Food, Sports) form distinct clusters — this is what "semantic similarity" looks like visually.

### Tab 2: Documents (Real Embeddings)

This uses the Gemini API to generate **real 768-dimensional embeddings** from any text.

1. Type a title (e.g., `Operating Systems Notes`)
2. Paste any text — lecture notes, textbook paragraphs, Wikipedia articles
3. Click **⚡ EMBED & INSERT**
4. Long documents are automatically split into overlapping 250-word chunks
5. Each chunk gets its own embedding and is stored in a separate HNSW index

### Tab 3: Ask AI (RAG Pipeline)

1. Make sure you have inserted some documents in Tab 2 first
2. Type a question about your documents
3. Click **🤖 ASK AI**

What happens behind the scenes:
```
1. Your question → embedded with text-embedding-004 (768D vector)
2. HNSW search → finds 3 most semantically similar chunks
3. Retrieved chunks → sent as context to gemini-1.5-flash
4. gemini-1.5-flash → generates an answer based only on your documents
```

The answer streams in with a typewriter effect. Click the **context chips** to see exactly which chunks the AI used.

---

## Project Structure

```
NexusDB/
├── src/
│   └── vectordb/   ← Java backend (HNSW, KD-Tree, BruteForce, REST API, RAG)
├── index.html      ← Frontend (PCA scatter plot, chat UI, benchmark)
└── README.md       ← This file
```

### Architecture

```
BruteForce          O(N·d)      Exact, baseline
KDTree              O(log N)    Exact, axis-aligned partitioning
HNSW                O(log N)    Approximate, multilayer small-world graph

VectorDB            Unified interface over all 3 (16D demo vectors)
DocumentDB          HNSW-only index for real embeddings (768D)
GeminiClient        HTTP client → Google Generative Language API
```

---

## License

MIT — use this however you want.
