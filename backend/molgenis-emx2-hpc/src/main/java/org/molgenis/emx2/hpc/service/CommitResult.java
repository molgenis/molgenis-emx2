package org.molgenis.emx2.hpc.service;

import org.molgenis.emx2.Row;

/** Result of a commit attempt, distinguishing success, wrong state, and hash mismatch. */
public record CommitResult(Row artifact, String error) {
  public static CommitResult success(Row artifact) {
    return new CommitResult(artifact, null);
  }

  public static CommitResult wrongState(String detail) {
    return new CommitResult(null, detail);
  }

  public static CommitResult hashMismatch(String detail) {
    return new CommitResult(null, detail);
  }

  public boolean isSuccess() {
    return artifact != null;
  }

  public boolean isHashMismatch() {
    return error != null && error.startsWith("hash_mismatch:");
  }
}
