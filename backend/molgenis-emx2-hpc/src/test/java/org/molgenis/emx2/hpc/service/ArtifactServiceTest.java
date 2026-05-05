package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Row.row;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

/**
 * Tests for {@link ArtifactService#computeTreeHash} — the tree hash algorithm that produces
 * content-addressable artifact identifiers. Both Java (server) and Python (daemon) must agree on
 * this algorithm, so these tests also serve as cross-language conformance anchors.
 *
 * <p>Requirement coverage: REQ-ART-HASH-001, REQ-ART-HASH-002.
 */
class ArtifactServiceTest {

  private static String sha256(String content) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  // --- REQ-ART-HASH-001: single-file hash ---

  @Test
  void singleFileReturnsFileHash() {
    String fileHash = sha256("file content");
    List<Row> files = List.of(row("path", "result.csv", "sha256", fileHash));
    assertEquals(fileHash, ArtifactService.computeTreeHash(files));
  }

  @Test
  void emptyFileListReturnsNull() {
    assertNull(ArtifactService.computeTreeHash(List.of()));
  }

  // --- REQ-ART-HASH-002: multi-file tree hash ---

  @Test
  void multiFileComputesTreeHash() {
    String hashA = sha256("content-a");
    String hashB = sha256("content-b");

    List<Row> files =
        List.of(
            row("path", "alpha.txt", "sha256", hashA), row("path", "beta.txt", "sha256", hashB));

    String expected = sha256("alpha.txt:" + hashA + "beta.txt:" + hashB);
    assertEquals(expected, ArtifactService.computeTreeHash(files));
  }

  @Test
  void multiFileSortsByPath() {
    String hashA = sha256("aaa");
    String hashZ = sha256("zzz");

    // Provide in reverse order — should be sorted by path
    List<Row> files =
        List.of(
            row("path", "zebra.txt", "sha256", hashZ), row("path", "alpha.txt", "sha256", hashA));

    String expected = sha256("alpha.txt:" + hashA + "zebra.txt:" + hashZ);
    assertEquals(expected, ArtifactService.computeTreeHash(files));
  }

  @Test
  void multiFileIsNotNaiveConcatenation() {
    String hashA = sha256("content-a");
    String hashB = sha256("content-b");

    List<Row> files =
        List.of(row("path", "a.txt", "sha256", hashA), row("path", "b.txt", "sha256", hashB));

    String treeHash = ArtifactService.computeTreeHash(files);
    String naiveHash = sha256("content-a" + "content-b");
    assertNotEquals(naiveHash, treeHash, "Tree hash must not be naive content concatenation");
  }

  @Test
  void treeHashIsDeterministic() {
    String hashA = sha256("aaa");
    String hashB = sha256("bbb");

    List<Row> files1 =
        List.of(row("path", "a.txt", "sha256", hashA), row("path", "b.txt", "sha256", hashB));
    List<Row> files2 =
        List.of(row("path", "b.txt", "sha256", hashB), row("path", "a.txt", "sha256", hashA));

    assertEquals(
        ArtifactService.computeTreeHash(files1),
        ArtifactService.computeTreeHash(files2),
        "Same files in different order should produce same tree hash");
  }

  @Test
  void nestedPathsAreHandledCorrectly() {
    String hashA = sha256("nested content");
    String hashB = sha256("root content");

    List<Row> files =
        List.of(
            row("path", "subdir/nested/file.csv", "sha256", hashA),
            row("path", "root.txt", "sha256", hashB));

    // Sorted: root.txt < subdir/nested/file.csv
    String expected = sha256("root.txt:" + hashB + "subdir/nested/file.csv:" + hashA);
    assertEquals(expected, ArtifactService.computeTreeHash(files));
  }

  @Test
  void pythonAndJavaTreeHashAgree() {
    // This test uses the exact same values as TestTreeHashComputation in test_daemon.py
    // to verify cross-language agreement.
    String hashA = sha256("content-a");
    String hashB = sha256("content-b");

    List<Row> files =
        List.of(row("path", "a.log", "sha256", hashA), row("path", "b.log", "sha256", hashB));

    String javaResult = ArtifactService.computeTreeHash(files);

    // Python reference: SHA256(concat(sorted "path:sha256hex"))
    String expectedInput = "a.log:" + hashA + "b.log:" + hashB;
    String pythonReference = sha256(expectedInput);

    assertEquals(pythonReference, javaResult, "Java and Python tree hash must agree");
  }
}
