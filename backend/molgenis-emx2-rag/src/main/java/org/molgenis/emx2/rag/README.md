# Rag demo 

Module includes tools to set up Retrieval-Augmented Generation (**RAG**) sample using the catalogue.

It contains the following parts:

- **HugginFaceDownloader**: A runnable tool to download the embedding model from HuggingFace and save it in the local file system. 
This is necessary because the embedding model will be used to generate embeddings for the catalogue data,
which will then be stored in a vector database for retrieval.

- **Embeddor**: A runnable tool to generate embeddings for the catalogue data using the downloaded embedding model. 
The generated embeddings will be stored in a vector database for retrieval.

- **Main**: A runnable class to demonstrate the RAG process, including downloading the embedding model, generating embeddings for the catalogue data, and performing retrieval using the generated embeddings.

The service can be tested via the web interface at ````/ui/rag-demo````. 

The server needs a acces to a postgres database with vector support. 
The database can be run via docker using the following command:

```bash
docker run -d --name emx-2-vector -p 5436:5432 -e POST
```

See EmbeddingStoreFactory class for details for the connection 

If the embeddor is run, it used the catalogue sitemap the index the entire catalogue website and generate embeddings for all the pages.

(_Note: This process can take a long time, especially if the catalogue contains a large number of pages._)

- **PARSE_LIMIT** is used to set the maximum number of pages to parse and generate embeddings for ( setting to 0 results in all pages being indexed).

TODO: 

- fix downloading and using alternative embedding models (e.g. sentence-transformers)
  - note: The huggingface onnx model may not work in java as java is more script then the python runtime then it comes to model structure and loading. the following script re-export the model in the correct format for java: 
```from optimum.onnxruntime import ORTModelForFeatureExtraction

model_id = "BAAI/bge-small-en-v1.5"
model = ORTModelForFeatureExtraction.from_pretrained(
model_id,
export=True
)
model.save_pretrained("clean_onnx")
```

- move pg vector stuff into docker compose for easy dev setup