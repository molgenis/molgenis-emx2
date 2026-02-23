package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.model.HpcJobStatus;

class LinkBuilderTest {

  @Test
  void pendingJobHasClaimAndCancelLinks() {
    Map<String, HateoasLink> links = LinkBuilder.forJob("job-1", HpcJobStatus.PENDING);
    assertNotNull(links.get("self"));
    assertNotNull(links.get("claim"));
    assertNotNull(links.get("cancel"));
    assertEquals("POST", links.get("claim").method());
    assertTrue(links.get("claim").href().contains("claim"));
  }

  @Test
  void startedJobHasCompleteFailCancelLinks() {
    Map<String, HateoasLink> links = LinkBuilder.forJob("job-1", HpcJobStatus.STARTED);
    assertNotNull(links.get("complete"));
    assertNotNull(links.get("fail"));
    assertNotNull(links.get("cancel"));
  }

  @Test
  void completedJobHasOnlySelfAndTransitionsLinks() {
    Map<String, HateoasLink> links = LinkBuilder.forJob("job-1", HpcJobStatus.COMPLETED);
    assertNotNull(links.get("self"));
    assertNotNull(links.get("transitions"));
    assertNull(links.get("claim"));
    assertNull(links.get("cancel"));
  }

  @Test
  void uploadingArtifactHasUploadAndCommitLinks() {
    Map<String, HateoasLink> links = LinkBuilder.forArtifact("art-1", ArtifactStatus.UPLOADING);
    assertNotNull(links.get("upload"));
    assertNotNull(links.get("commit"));
    assertNotNull(links.get("files"));
  }

  @Test
  void committedArtifactHasOnlySelfAndFilesLinks() {
    Map<String, HateoasLink> links = LinkBuilder.forArtifact("art-1", ArtifactStatus.COMMITTED);
    assertNotNull(links.get("self"));
    assertNotNull(links.get("files"));
    assertNull(links.get("upload"));
    assertNull(links.get("commit"));
  }
}
