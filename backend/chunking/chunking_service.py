from injector import singleton
from typing import List
import pickle
import os

from langchain.schema.document import Document
from unstructured.partition.pdf import partition_pdf
from unstructured.chunking.title import chunk_by_title
from unstructured.documents.elements import Element

@singleton
class ChunkingService:
    def __init__(self) -> None:
        pass

    def clean_metadata(self, metadata: dict) -> dict:
        """
        Clean metadata to ensure compatibility with Qdrant.
        """
        clean_meta = {}
        for key, value in metadata.items():
            if key == "coordinates":
                # Convert coordinates to a simple list of tuples
                clean_meta[key] = {
                    "points": [list(point) for point in value.get("points", [])],
                    "system": value.get("system", ""),
                    "layout_width": value.get("layout_width", 0),
                    "layout_height": value.get("layout_height", 0)
                }
            elif isinstance(value, (str, int, float, bool)):
                clean_meta[key] = value
            else:
                clean_meta[key] = str(value)  # Convert unsupported types to string
        return clean_meta

    def get_chunks_from_pdf(self, 
                            file_path: str, 
                            strategy: str = "hi_res", 
                            languages: List[str] = ["fra"],
                            new_after_n_chars: float = 2500,
                            multipage_sections: bool = True,
                            combine_text_under_n_chars: float = 500,
                            max_characters: float = 4000) -> list[Element]:

        # Générer le nom du fichier pickle
        pickle_file_path = f"./data/{os.path.splitext(os.path.basename(file_path))[0][:20]}.pickle"

        # Vérifier si le fichier pickle existe déjà
        if os.path.exists(pickle_file_path):
            print(f"Le fichier pickle {pickle_file_path} existe déjà, chargement des chunks...")
            with open(pickle_file_path, 'rb') as file:
                chunks = pickle.load(file)
            return chunks

        # Si le fichier pickle n'existe pas, générer les chunks
        print(f"On partitionne le pdf")
        elements = partition_pdf(file_path, strategy=strategy, languages=languages)
        print(f"On chunk le pdf")
        chunks = chunk_by_title(elements, new_after_n_chars=new_after_n_chars, 
                                multipage_sections=multipage_sections, combine_text_under_n_chars=combine_text_under_n_chars, 
                                max_characters=max_characters)

        # Sauvegarder les chunks dans un fichier pickle
        with open(pickle_file_path, 'wb') as file:
            pickle.dump(chunks, file)

        return chunks

    def get_chunked_documents_from_pdf(self, 
                                        path: str, 
                                        strategy: str = "hi_res", 
                                        languages: List[str] = ["fra"],
                                        new_after_n_chars: float = 2500,
                                        multipage_sections: bool = True,
                                        combine_text_under_n_chars: float = 500,
                                        max_characters: float = 4000) -> List[Document]:
        
        chunks_list = []
        if os.path.isfile(path):
            chunks = self.get_chunks_from_pdf(file_path=path,
                                            strategy=strategy,
                                            languages=languages,
                                            new_after_n_chars=new_after_n_chars,
                                            multipage_sections=multipage_sections,
                                            combine_text_under_n_chars=combine_text_under_n_chars,
                                            max_characters=max_characters)
            chunks_list.append(chunks)
            
        elif os.path.isdir(path):
            pdf_files = [os.path.join(path, f) for f in os.listdir(path) if f.endswith('.pdf')]
            for pdf_path in pdf_files:
                chunks = self.get_chunks_from_pdf(file_path=pdf_path,
                                            strategy=strategy,
                                            languages=languages,
                                            new_after_n_chars=new_after_n_chars,
                                            multipage_sections=multipage_sections,
                                            combine_text_under_n_chars=combine_text_under_n_chars,
                                            max_characters=max_characters)
                for chunk in chunks:
                    chunks_list.append(chunk)
                
        n = 0
        documents = []
        print(f"Chunk List  === {chunks_list}")
        for chunk in chunks_list:
            try:
                metadata = chunk.metadata.to_dict()
                metadata["source"] = metadata["filename"]
                metadata["chunk_number"] = str(n)
                clean_meta = self.clean_metadata(metadata)
                documents.append(Document(page_content=chunk.text, metadata=clean_meta))
                n += 1
            except Exception as e:
                print(f"Erreur lors du passage des chunks aux documents : {e}")
        return documents

    def save_chunked_documents_to_pickle(self, file_path, documents, file_name = ""):
        if file_name:
            pickle_file_path = f"./data/{os.path.splitext(file_name)}.pickle"
        else:
            pickle_file_path = f"./data/{os.path.splitext(os.path.basename(file_path))[0][:20]}.pickle"

        if os.path.exists(pickle_file_path):
            print(f"Le fichier pickle {pickle_file_path} existe déjà")
        else:
            with open(pickle_file_path, 'wb') as file:
                pickle.dump(documents, file)
        return pickle_file_path
