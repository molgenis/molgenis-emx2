package org.molgenis.emx2.hpc.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.model.HpcJobStatus;

/**
 * Builds HATEOAS _links maps based on current job or artifact state. Clients should follow these
 * links rather than constructing URLs.
 */
public final class LinkBuilder {

  private static final String BASE = "/api/hpc";

  private LinkBuilder() {}

  /**
   * Returns links available for a job in the given status.
   *
   * <p>E.g., a PENDING job has "claim" and "cancel" links; a CLAIMED job has "submit" and "cancel";
   * a terminal job has only "self".
   */
  public static Map<String, HateoasLink> forJob(String jobId, HpcJobStatus status) {
    Map<String, HateoasLink> links = new LinkedHashMap<>();
    links.put("self", HateoasLink.get(BASE + "/jobs/" + jobId));

    switch (status) {
      case PENDING -> {
        links.put("claim", HateoasLink.post(BASE + "/jobs/" + jobId + "/claim"));
        links.put("cancel", HateoasLink.post(BASE + "/jobs/" + jobId + "/cancel"));
      }
      case CLAIMED -> {
        links.put("submit", HateoasLink.post(BASE + "/jobs/" + jobId + "/transition"));
        links.put("cancel", HateoasLink.post(BASE + "/jobs/" + jobId + "/cancel"));
      }
      case SUBMITTED -> {
        links.put("start", HateoasLink.post(BASE + "/jobs/" + jobId + "/transition"));
        links.put("cancel", HateoasLink.post(BASE + "/jobs/" + jobId + "/cancel"));
      }
      case STARTED -> {
        links.put("complete", HateoasLink.post(BASE + "/jobs/" + jobId + "/transition"));
        links.put("fail", HateoasLink.post(BASE + "/jobs/" + jobId + "/transition"));
        links.put("cancel", HateoasLink.post(BASE + "/jobs/" + jobId + "/cancel"));
      }
        // Terminal states: only self link
      default -> {}
    }

    links.put("transitions", HateoasLink.get(BASE + "/jobs/" + jobId + "/transitions"));
    return links;
  }

  /** Returns links available for an artifact in the given status. */
  public static Map<String, HateoasLink> forArtifact(String artifactId, ArtifactStatus status) {
    Map<String, HateoasLink> links = new LinkedHashMap<>();
    links.put("self", HateoasLink.get(BASE + "/artifacts/" + artifactId));

    switch (status) {
      case CREATED -> {
        links.put("upload", HateoasLink.put(BASE + "/artifacts/" + artifactId + "/files/{path}"));
        links.put("upload_legacy", HateoasLink.post(BASE + "/artifacts/" + artifactId + "/files"));
      }
      case UPLOADING -> {
        links.put("upload", HateoasLink.put(BASE + "/artifacts/" + artifactId + "/files/{path}"));
        links.put("upload_legacy", HateoasLink.post(BASE + "/artifacts/" + artifactId + "/files"));
        links.put("commit", HateoasLink.post(BASE + "/artifacts/" + artifactId + "/commit"));
      }
      case REGISTERED -> {
        links.put("commit", HateoasLink.post(BASE + "/artifacts/" + artifactId + "/commit"));
      }
      case COMMITTED -> {
        links.put("download", HateoasLink.get(BASE + "/artifacts/" + artifactId + "/files/{path}"));
      }
      default -> {}
    }

    links.put("files", HateoasLink.get(BASE + "/artifacts/" + artifactId + "/files"));
    return links;
  }
}
