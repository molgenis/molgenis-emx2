package org.molgenis.emx2.rag;

import static org.molgenis.emx2.rag.RagConstants.*;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class RagModelFactory {

  private RagModelFactory() {}

  public static EmbeddingModel create() throws IOException {
    String modelBase = MODEL_PATH + "/" + MODEL_NAME + "/";

    InputStream modelStream =
        Files.newInputStream(Path.of(modelBase + MODEL_FILE_NAME + MODEL_FILE_EXTENTION));

    Path modelPath = Files.createTempFile(MODEL_NAME, MODEL_FILE_EXTENTION);

    Files.copy(modelStream, modelPath, StandardCopyOption.REPLACE_EXISTING);

    InputStream tokenizerStream = Files.newInputStream(Path.of(modelBase + "tokenizer.json"));

    Path tokenizerPath = Files.createTempFile("tokenizer", ".json");

    Files.copy(tokenizerStream, tokenizerPath, StandardCopyOption.REPLACE_EXISTING);

    System.out.println(modelPath);
    System.out.println(tokenizerPath);

    PoolingMode poolingMode = PoolingMode.MEAN;
    EmbeddingModel embeddingModel = new OnnxEmbeddingModel(modelPath, tokenizerPath, poolingMode);
    return embeddingModel;
  }
}
