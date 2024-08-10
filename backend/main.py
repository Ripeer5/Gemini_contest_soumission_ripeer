from fastapi import FastAPI, Depends, Request
from fastapi.middleware.cors import CORSMiddleware

from gemini.gemini_router import gemini_router
from health.health_router import health_router
from gemini.gemini_service import GeminiService
from vectorstore.qdrant_service import QdrantService
from chunking.chunking_service import ChunkingService
from embedding.embedding_service import EmbeddingService


class GlobalInjector:
    def __init__(self):
        self.chunking_service = ChunkingService()
        self.embedding_service = EmbeddingService()
        self.qdrant_service = QdrantService(embedding_service=self.embedding_service)
        self.gemini_service = GeminiService(qdrant_service=self.qdrant_service)


global_injector = GlobalInjector()

def create_app(global_injector) -> FastAPI:
    async def bind_injector_to_request(request: Request) -> None:
        request.state.injector = global_injector

    app = FastAPI(dependencies=[Depends(bind_injector_to_request)])

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    app.include_router(gemini_router, prefix="/gemini", tags=["gemini"])
    app.include_router(health_router, prefix="/health")

    return app

app = create_app(global_injector)

@app.get("/")
def read_root():
    return {"message": "Welcome to the Gemini API"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
