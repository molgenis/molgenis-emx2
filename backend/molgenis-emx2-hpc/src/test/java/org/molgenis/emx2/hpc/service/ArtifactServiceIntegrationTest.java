package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.ArtifactStatus;

class ArtifactServiceIntegrationTest extends HpcServiceIntegrationTestBase {

  @Test
  void commitRejectsHashMismatchAndKeepsArtifactMutable() {
    String artifactId =
        artifactService.createArtifact(
            "input", "dataset", "managed", null, "{\"source\":\"test\"}");

    byte[] payload = "artifact-bytes".getBytes(StandardCharsets.UTF_8);
    String fileHash = sha256Hex(payload);
    artifactService.uploadFileByPath(
        artifactId,
        "results/data.txt",
        fileHash,
        (long) payload.length,
        "text/plain",
        new BinaryFileWrapper("text/plain", "data.txt", payload));

    CommitResult mismatch = artifactService.commitArtifact(artifactId, "deadbeef", null);
    assertNotNull(mismatch);
    assertFalse(mismatch.isSuccess());
    assertTrue(mismatch.isHashMismatch());

    Row stillUploading = artifactService.getArtifact(artifactId);
    assertEquals(ArtifactStatus.UPLOADING.name(), stillUploading.getString("status"));

    CommitResult success =
        artifactService.commitArtifact(artifactId, fileHash, (long) payload.length);
    assertNotNull(success);
    assertTrue(success.isSuccess());

    Row committed = artifactService.getArtifact(artifactId);
    assertEquals(ArtifactStatus.COMMITTED.name(), committed.getString("status"));
    assertEquals(fileHash, committed.getString("sha256"));
  }

  @Test
  void committedArtifactsAreImmutable() {
    String artifactId = artifactService.createArtifact("immutable", "blob", "managed", null, null);

    byte[] first = "first-version".getBytes(StandardCharsets.UTF_8);
    String firstHash = sha256Hex(first);
    artifactService.uploadFileByPath(
        artifactId,
        "output.txt",
        firstHash,
        (long) first.length,
        "text/plain",
        new BinaryFileWrapper("text/plain", "output.txt", first));

    assertTrue(
        artifactService.commitArtifact(artifactId, firstHash, (long) first.length).isSuccess());

    byte[] replacement = "replacement".getBytes(StandardCharsets.UTF_8);
    assertThrows(
        MolgenisException.class,
        () ->
            artifactService.uploadFileByPath(
                artifactId,
                "output.txt",
                sha256Hex(replacement),
                (long) replacement.length,
                "text/plain",
                new BinaryFileWrapper("text/plain", "output.txt", replacement)));

    assertThrows(
        MolgenisException.class, () -> artifactService.deleteFile(artifactId, "output.txt"));

    Row file = artifactService.getFileMetadata(artifactId, "output.txt");
    assertEquals(firstHash, file.getString("sha256"));
    assertEquals(String.valueOf(first.length), file.getString("size_bytes"));
  }

  @Test
  void commitWithoutClientHashComputesCanonicalTreeHashAndSize() {
    String artifactId = artifactService.createArtifact("multi", "blob", "managed", null, null);

    byte[] alpha = "alpha-content".getBytes(StandardCharsets.UTF_8);
    byte[] beta = "beta-content".getBytes(StandardCharsets.UTF_8);
    String alphaHash = sha256Hex(alpha);
    String betaHash = sha256Hex(beta);

    artifactService.uploadFileByPath(
        artifactId,
        "nested/a.txt",
        alphaHash,
        (long) alpha.length,
        "text/plain",
        new BinaryFileWrapper("text/plain", "a.txt", alpha));
    artifactService.uploadFileByPath(
        artifactId,
        "nested/b.txt",
        betaHash,
        (long) beta.length,
        "text/plain",
        new BinaryFileWrapper("text/plain", "b.txt", beta));

    CommitResult committed = artifactService.commitArtifact(artifactId, null, null);
    assertNotNull(committed);
    assertTrue(committed.isSuccess());

    Row artifact = artifactService.getArtifact(artifactId);
    assertEquals(ArtifactStatus.COMMITTED.name(), artifact.getString("status"));

    String expectedTreeHash = treeHash(Map.of("nested/a.txt", alphaHash, "nested/b.txt", betaHash));
    assertEquals(expectedTreeHash, artifact.getString("sha256"));

    long expectedSize = alpha.length + beta.length;
    assertEquals(expectedSize, Long.parseLong(artifact.getString("size_bytes")));
  }

  private static String treeHash(Map<String, String> pathToSha256) {
    if (pathToSha256.isEmpty()) {
      return null;
    }
    if (pathToSha256.size() == 1) {
      return pathToSha256.values().iterator().next();
    }

    String joined =
        pathToSha256.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(e -> e.getKey() + ":" + e.getValue())
            .reduce("", String::concat);
    return sha256Hex(joined.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
