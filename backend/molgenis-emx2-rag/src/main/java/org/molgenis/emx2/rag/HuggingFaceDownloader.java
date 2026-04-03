package org.molgenis.emx2.rag;

import static org.molgenis.emx2.rag.RagConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class HuggingFaceDownloader {

  private static final String[] FILES = {
    "vocab.txt",
    "tokenizer_config.json",
    "tokenizer.json",
    "config.json",
    "model_optimized.onnx",
    "special_tokens_map.json"
  };

  public static void main(String[] args) throws Exception {

    Path targetDir = Path.of(MODEL_PATH, MODEL_NAME);

    if (Files.exists(targetDir)) {
      System.out.println("Target directory already exists: " + targetDir);
    } else {
      Files.createDirectories(targetDir);

      System.out.println("Target directory created: " + targetDir);
      HttpClient client =
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

      for (String file : FILES) {
        downloadFile(
            client, MODEL_REPOSITORY_URL + file + "?download=true", targetDir.resolve(file));
      }

      System.out.println("Download complete!");
    }
  }

  private static void downloadFile(HttpClient client, String url, Path path)
      throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

    HttpResponse<InputStream> response =
        client.send(request, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() == 200) {
      Files.copy(response.body(), path);
      System.out.println("Downloaded: " + path);
    } else {
      System.err.println("Failed: " + url + " (" + response.statusCode() + ")");
    }
  }
}
