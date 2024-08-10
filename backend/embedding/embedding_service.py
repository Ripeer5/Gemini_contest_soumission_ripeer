from injector import singleton, inject
from langchain.storage import LocalFileStore
from typing import List
from langchain.embeddings import CacheBackedEmbeddings
from langchain.schema.document import Document
from langchain_community.embeddings import HuggingFaceEmbeddings
from pathlib import Path



@singleton
class EmbeddingService:
    def __init__(self, model_name: str = "OrdalieTech/Solon-embeddings-large-0.1",
                 root_path: str = Path("/data/vectorstore/cache"),
                 embed_model_cache_path = r"./data/embedding/cache/",
                 
                 ) -> None:
        self.embeddings_model = HuggingFaceEmbeddings(model_name=model_name)
        self.document_store = LocalFileStore(root_path)
        self.embedder = CacheBackedEmbeddings.from_bytes_store(
                self.embeddings_model,
                self.document_store,
                namespace=embed_model_cache_path
            )
    
