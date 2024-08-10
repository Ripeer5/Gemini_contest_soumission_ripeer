from injector import singleton, inject
from langchain_qdrant import Qdrant
from qdrant_client import QdrantClient
from embedding.embedding_service import EmbeddingService

from typing import List
from langchain.schema.document import Document
from langchain_core.embeddings.embeddings import Embeddings
import uuid
import docker

@singleton
class QdrantService:
    def __init__(self, embedding_service: EmbeddingService, url: str = "http://localhost:6333/") -> None:
        self.url = url
        self.embeddings = embedding_service.embedder
        self.qdrant_client = QdrantClient(url=self.url)
        self.client = docker.from_env()
        self.ensure_container_running()

    def ensure_container_running(self):
        container_name = "qdrant"
        try:
            # Vérifier si le conteneur existe
            containers = self.client.containers.list(all=True, filters={"name": container_name})
            if not containers:
                # Lancer le conteneur s'il n'existe pas
                self.launch_container(container_name)
            else:
                container = containers[0]
                if container.status != 'running':
                    # Démarrer le conteneur s'il est arrêté
                    container.start()
                    print(f"Conteneur {container_name} démarré.")
        except docker.errors.DockerException as e:
            print(f"Erreur lors de la vérification ou du lancement du conteneur: {e}")

    def launch_container(self, container_name: str):
        try:
            self.client.containers.run(
                "qdrant/qdrant",
                detach=True,
                name=container_name,
                ports={'6333/tcp': 6333, '6334/tcp': 6334},
                volumes={"D:\\Boulot\\Intelligence_artificielle\\Gemini\\Backend\\GeminiAppBackend/qdrant_storage": {'bind': '/qdrant/storage', 'mode': 'z'}}
            )
            print(f"Conteneur {container_name} lancé avec succès.")
        except docker.errors.DockerException as e:
            print(f"Erreur lors du lancement du conteneur: {e}")

    def create_collection(self, docs: List[Document], collection_name: str = str(uuid.uuid4())):       
        if self.qdrant_client.collection_exists(collection_name):
            print(f"Collection '{collection_name}' already exists.")
        else:
            Qdrant.from_documents(
                docs,
                embedding=self.embeddings,
                url=self.url,
                prefer_grpc=True,
                collection_name=collection_name,
            )
            print(f"Collection '{collection_name}' created successfully.")
        
    def get_relevant_documents_from_collection(self, query: str, collection_name: str):
        doc_store = Qdrant.from_existing_collection(
            collection_name=collection_name,
            embedding=self.embeddings,
            path=None,
            url=self.url,
        )
        
        found_docs = doc_store.similarity_search(query, k=6)
        return found_docs
    
    def delete_collection(self, collection_name: str):
        self.qdrant_client.delete_collection(collection_name=collection_name)
        
    async def get_relevant_documents_and_neighbor_from_collection(self, query: str, collection_name: str):
        all_documents = self.get_documents_from_collection(collection_name=collection_name)
        retrieved_documents = self.get_relevant_documents_from_collection(query=query, collection_name=collection_name)
        chunk_numbers = [int(doc.metadata["chunk_number"]) for doc in all_documents]
        return_documents = list(retrieved_documents)  
        
        for doc in retrieved_documents:
            chunk_number = int(doc.metadata["chunk_number"])
            
            if chunk_number > 0:
                previous_docs = [document for document in all_documents if int(document.metadata["chunk_number"]) == chunk_number - 1]
                return_documents.append(previous_docs[0])
            
            if chunk_number < max(chunk_numbers):
                next_docs = [document for document in all_documents if int(document.metadata["chunk_number"]) == chunk_number + 1]
                return_documents.append(next_docs[0])
        
        clean_documents = self.clean_document_retrieved(raw_documents=return_documents)
                
        return clean_documents

        
    def get_documents_from_collection(self, collection_name: str):        
        
        # Récupérer tous les points (documents) de la collection
        all_raw_documents = []
        limit = 100  # Nombre de documents à récupérer par requête

        points, scroll_id = self.qdrant_client.scroll(
            collection_name=collection_name,
            limit=limit,
        )
        
        all_raw_documents.extend(points)

        while scroll_id:
            points, scroll_id = self.qdrant_client.scroll(
                collection_name=collection_name,
                scroll=scroll_id,
                limit=limit,
            )
            all_raw_documents.extend(points)
        
        documents = []
        for raw_doc in all_raw_documents:
            page_content = raw_doc.payload['page_content']
            metadata = raw_doc.payload['metadata']
            doc = Document(page_content=page_content, metadata=metadata)
            documents.append(doc)
        
        clean_documents = self.clean_document_retrieved(raw_documents=documents)
        return clean_documents
    
    @staticmethod
    def clean_document_retrieved(raw_documents: List[Document]) -> List[Document]:
        unique_documents = {}
        for doc in raw_documents:
            chunk_number = doc.metadata.get('chunk_number')
            if chunk_number is not None and chunk_number not in unique_documents:
                unique_documents[chunk_number] = doc
        
        # Convertir le dictionnaire en liste et trier par chunk_number
        sorted_documents = sorted(unique_documents.values(), key=lambda doc: doc.metadata['chunk_number'])
        
        return sorted_documents
    
    def get_collections_names(self):
        # Récupération des collections
        collections = self.qdrant_client.get_collections()

        # Extraction des noms des collections
        collection_names = [collection.name for collection in collections.collections]
        return collection_names
