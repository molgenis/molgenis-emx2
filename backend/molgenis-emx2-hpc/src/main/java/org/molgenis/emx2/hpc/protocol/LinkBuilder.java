package org.molgenis.emx2.hpc.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.model.HpcJobStatus;

/**
 * Builds HATEOAS _links maps based on current job or artifact state. Clients should follow these
 * links rather than constructing URLs.
 */
// All route fragments below are part of the HPC protocol contract (see docs/hpc/design.md);
// they are not user-configurable URIs.
@SuppressWarnings("java:S1075")
public final class LinkBuilder {

  private static final String BASE = "/api/hpc";
  private static final String JOBS = "/jobs/";
  private static final String ARTIFACTS = "/artifacts/";
  private static final String TRANSITION = "/transition";
  private static final String CANCEL = "/cancel";
  private static final String CANCEL_REL = "cancel";
  private static final String FILES_PATH_TEMPLATE = "/files/{path}";

  private LinkBuilder() {}

  /**
   * Returns links available for a job in the given status.
   *
   * <p>E.g., a PENDING job has "claim" and "cancel" links; a CLAIMED job has "submit" and "cancel";
   * a terminal job has only "self".
   */
  public static Map<String, HateoasLink> forJob(String jobId, HpcJobStatus status) {
    Map<String, HateoasLink> links = new LinkedHashMap<>();
    String jobPath = BASE + JOBS + jobId;
    links.put("self", HateoasLink.get(jobPath));

    switch (status) {
      case PENDING -> {
        links.put("claim", HateoasLink.post(jobPath + "/claim"));
        links.put(CANCEL_REL, HateoasLink.post(jobPath + CANCEL));
      }
      case CLAIMED -> {
        links.put("submit", HateoasLink.post(jobPath + TRANSITION));
        links.put(CANCEL_REL, HateoasLink.post(jobPath + CANCEL));
      }
      case SUBMITTED -> {
        links.put("start", HateoasLink.post(jobPath + TRANSITION));
        links.put(CANCEL_REL, HateoasLink.post(jobPath + CANCEL));
      }
      case STARTED -> {
        links.put("complete", HateoasLink.post(jobPath + TRANSITION));
        links.put("fail", HateoasLink.post(jobPath + TRANSITION));
        links.put(CANCEL_REL, HateoasLink.post(jobPath + CANCEL));
      }
      default -> {
        // Terminal states: only self and transitions links
      }
    }

    links.put("transitions", HateoasLink.get(jobPath + "/transitions"));
    return links;
  }

  /** Returns links available for an artifact in the given status. */
  public static Map<String, HateoasLink> forArtifact(String artifactId, ArtifactStatus status) {
    Map<String, HateoasLink> links = new LinkedHashMap<>();
    String artPath = BASE + ARTIFACTS + artifactId;
    links.put("self", HateoasLink.get(artPath));

    switch (status) {
      case CREATED -> links.put("upload", HateoasLink.put(artPath + FILES_PATH_TEMPLATE));
      case UPLOADING -> {
        links.put("upload", HateoasLink.put(artPath + FILES_PATH_TEMPLATE));
        links.put("commit", HateoasLink.post(artPath + "/commit"));
      }
      case REGISTERED -> links.put("commit", HateoasLink.post(artPath + "/commit"));
      case COMMITTED -> links.put("download", HateoasLink.get(artPath + FILES_PATH_TEMPLATE));
      default -> {
        // Terminal FAILED state: only self and files links
      }
    }

    links.put("files", HateoasLink.get(artPath + "/files"));
    return links;
  }
}
