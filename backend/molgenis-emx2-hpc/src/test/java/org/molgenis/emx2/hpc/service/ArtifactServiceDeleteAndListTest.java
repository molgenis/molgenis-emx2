package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.ArtifactStatus;

class ArtifactServiceDeleteAndListTest extends HpcServiceIntegrationTestBase {

  @Test
  void deleteArtifact_removesArtifactAndFiles() {
    String id = artifactService.createArtifact("to-delete", "blob", "managed", null, null);
    uploadTestFile(id, "file.txt", "data");
    artifactService.commitArtifact(id, null, null);

    Row deleted = artifactService.deleteArtifact(id);
    assertNotNull(deleted);
    assertNull(artifactService.getArtifact(id));
    assertTrue(artifactService.listFiles(id, null, 100, 0).isEmpty());
  }

  @Test
  void deleteArtifact_returnsNullForNonExistent() {
    assertNull(artifactService.deleteArtifact("nonexistent-id"));
  }

  @Test
  void deleteArtifact_nullifiesJobReferences() {
    String artId = artifactService.createArtifact("output", "blob", "managed", null, null);
    uploadTestFile(artId, "out.txt", "data");
    artifactService.commitArtifact(artId, null, null);

    String jobId = jobService.createJob("proc", "p", null, null, null, null);
    // Manually set output_artifact_id via transition
    workerService.registerOrHeartbeat(
        "w", "host", List.of(java.util.Map.of("processor", "proc", "profile", "p")));
    jobService.claimJob(jobId, "w");
    jobService.transitionJob(
        jobId,
        org.molgenis.emx2.hpc.model.HpcJobStatus.SUBMITTED,
        new TransitionParams("w", "submitted", null, artId, null, null, null, null));

    // Delete artifact should nullify the reference
    artifactService.deleteArtifact(artId);
    Row job = jobService.getJob(jobId);
    assertNull(job.getString("output_artifact_id"));
  }

  @Test
  void getArtifact_returnsNullForMissing() {
    assertNull(artifactService.getArtifact("nonexistent"));
  }

  @Test
  void listFiles_supportsPrefixFilter() {
    String id = artifactService.createArtifact("multi", "blob", "managed", null, null);
    uploadTestFile(id, "data/a.txt", "a");
    uploadTestFile(id, "data/b.txt", "b");
    uploadTestFile(id, "logs/c.txt", "c");

    List<Row> dataFiles = artifactService.listFiles(id, "data/", 100, 0);
    assertEquals(2, dataFiles.size());

    List<Row> logFiles = artifactService.listFiles(id, "logs/", 100, 0);
    assertEquals(1, logFiles.size());
  }

  @Test
  void listFiles_supportsPagination() {
    String id = artifactService.createArtifact("paginate", "blob", "managed", null, null);
    for (int i = 0; i < 5; i++) {
      uploadTestFile(id, "file" + i + ".txt", "data" + i);
    }

    List<Row> page1 = artifactService.listFiles(id, null, 2, 0);
    List<Row> page2 = artifactService.listFiles(id, null, 2, 2);
    assertEquals(2, page1.size());
    assertEquals(2, page2.size());
  }

  @Test
  void countFiles_matchesList() {
    String id = artifactService.createArtifact("count", "blob", "managed", null, null);
    uploadTestFile(id, "a.txt", "a");
    uploadTestFile(id, "b.txt", "b");

    assertEquals(2, artifactService.countFiles(id, null));
    assertEquals(1, artifactService.countFiles(id, "a"));
  }

  @Test
  void getFileWithContent_returnsContentAndMetadata() {
    String id = artifactService.createArtifact("content-test", "blob", "managed", null, null);
    uploadTestFile(id, "readme.txt", "hello world");

    Row file = artifactService.getFileWithContent(id, "readme.txt");
    assertNotNull(file);
    assertEquals("readme.txt", file.getString("path"));
  }

  @Test
  void getFileWithContent_returnsNullForMissing() {
    String id = artifactService.createArtifact("missing-file", "blob", "managed", null, null);
    assertNull(artifactService.getFileWithContent(id, "no-such-file.txt"));
  }

  @Test
  void getFileMetadata_returnsMetadataWithoutContent() {
    String id = artifactService.createArtifact("metadata-test", "blob", "managed", null, null);
    uploadTestFile(id, "data.bin", "binary");

    Row metadata = artifactService.getFileMetadata(id, "data.bin");
    assertNotNull(metadata);
    assertNotNull(metadata.getString("sha256"));
  }

  @Test
  void uploadFileByPath_upsertReplacesExistingFile() {
    String id = artifactService.createArtifact("upsert", "blob", "managed", null, null);
    String firstId = uploadTestFile(id, "file.txt", "version1");
    String secondId = uploadTestFile(id, "file.txt", "version2");

    assertEquals(firstId, secondId, "Upsert should return same file ID");
    assertEquals(1, artifactService.listFiles(id, null, 100, 0).size());
  }

  @Test
  void uploadFileByPath_rejectsUploadToCommittedArtifact() {
    String id = artifactService.createArtifact("immutable", "blob", "managed", null, null);
    uploadTestFile(id, "file.txt", "data");
    artifactService.commitArtifact(id, null, null);

    assertThrows(MolgenisException.class, () -> uploadTestFile(id, "new.txt", "data"));
  }

  @Test
  void deleteFile_removesFileBeforeCommit() {
    String id = artifactService.createArtifact("delete-file", "blob", "managed", null, null);
    uploadTestFile(id, "temp.txt", "temp");

    assertTrue(artifactService.deleteFile(id, "temp.txt"));
    assertFalse(artifactService.deleteFile(id, "temp.txt"));
    assertEquals(0, artifactService.listFiles(id, null, 100, 0).size());
  }

  @Test
  void deleteFile_rejectsOnCommittedArtifact() {
    String id = artifactService.createArtifact("committed", "blob", "managed", null, null);
    uploadTestFile(id, "file.txt", "data");
    artifactService.commitArtifact(id, null, null);

    assertThrows(MolgenisException.class, () -> artifactService.deleteFile(id, "file.txt"));
  }

  @Test
  void createArtifact_externalResidenceStartsAsRegistered() {
    String id =
        artifactService.createArtifact("ext", "dataset", "posix", "file:///data/file", null);
    Row artifact = artifactService.getArtifact(id);
    assertEquals(ArtifactStatus.REGISTERED.name(), artifact.getString("status"));
  }

  @Test
  void commitArtifact_returnsNullForNonExistent() {
    assertNull(artifactService.commitArtifact("nonexistent", null, null));
  }

  @Test
  void commitArtifact_rejectsAlreadyCommitted() {
    String id = artifactService.createArtifact("double-commit", "blob", "managed", null, null);
    uploadTestFile(id, "f.txt", "d");
    assertTrue(artifactService.commitArtifact(id, null, null).isSuccess());

    CommitResult second = artifactService.commitArtifact(id, null, null);
    assertNotNull(second);
    assertFalse(second.isSuccess());
  }

  private String uploadTestFile(String artifactId, String path, String content) {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    String hash = sha256Hex(bytes);
    return artifactService.uploadFileByPath(
        artifactId,
        path,
        hash,
        (long) bytes.length,
        "text/plain",
        new BinaryFileWrapper("text/plain", path, bytes));
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
