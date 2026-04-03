package org.molgenis.emx2.rag;

public class RagConstants {
  public static final String MODEL_REPOSITORY_URL =
      "https://huggingface.co/llmware/bge-small-en-v1.5-onnx/resolve/main/";
  public static final String MODEL_PATH = "data/models";
  public static final String MODEL_NAME = "bge-small-en-onnx";
  public static final String CATALOGUE_LOCATION =
      "https://molgeniscatalogue.org/catalogue/sitemap.xml";
  public static final int PARSE_LIMIT = 10;
  public static final String MODEL_FILE_NAME = "model";
  public static final String MODEL_FILE_EXTENTION = ".onnx";
}
