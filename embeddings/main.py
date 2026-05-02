from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import numpy as np
import logging
import os

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="LearnAI Embedding Service", version="1.0.0")

# Load model once at startup — stays in memory
# all-MiniLM-L6-v2 = 384 dimensions, ~80MB RAM, fast
logger.info("Loading sentence-transformers/all-MiniLM-L6-v2...")
model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
logger.info("Embedding model loaded — ready")

# ── Request/Response ──────────────────────────────────────────────────────────

class EmbedRequest(BaseModel):
    text: str

class EmbedResponse(BaseModel):
    embedding: list[float]
    dimensions: int

class BatchEmbedRequest(BaseModel):
    texts: list[str]

class BatchEmbedResponse(BaseModel):
    embeddings: list[list[float]]
    dimensions: int
    count: int

# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "UP", "model": "all-MiniLM-L6-v2", "dimensions": 384}

@app.post("/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest):
    """Embed a single text string → 384-dim float vector"""
    if not request.text or not request.text.strip():
        return EmbedResponse(embedding=[0.0] * 384, dimensions=384)

    vector = model.encode(request.text, normalize_embeddings=True)
    return EmbedResponse(
        embedding=vector.tolist(),
        dimensions=len(vector)
    )

@app.post("/embed/batch", response_model=BatchEmbedResponse)
def embed_batch(request: BatchEmbedRequest):
    """Embed multiple texts in one call — more efficient than individual calls"""
    if not request.texts:
        return BatchEmbedResponse(embeddings=[], dimensions=384, count=0)

    vectors = model.encode(request.texts, normalize_embeddings=True)
    return BatchEmbedResponse(
        embeddings=[v.tolist() for v in vectors],
        dimensions=vectors.shape[1],
        count=len(vectors)
    )

# ── Startup ───────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 10000))
    uvicorn.run(app, host="0.0.0.0", port=port)