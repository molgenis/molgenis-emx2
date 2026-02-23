package org.molgenis.emx2.hpc.service;

import org.molgenis.emx2.Row;

/** Result of a commit attempt, distinguishing success, wrong state, and hash mismatch. */
public record CommitResult(Row artifact, String error, ErrorType errorType) {

  public enum ErrorType {
    NONE,
    WRONG_STATE,
    HASH_MISMATCH
  }

  public static CommitResult success(Row artifact) {
    return new CommitResult(artifact, null, ErrorType.NONE);
  }

  public static CommitResult wrongState(String detail) {
    return new CommitResult(null, detail, ErrorType.WRONG_STATE);
  }

  public static CommitResult hashMismatch(String detail) {
    return new CommitResult(null, detail, ErrorType.HASH_MISMATCH);
  }

  public boolean isSuccess() {
    return artifact != null;
  }

  public boolean isHashMismatch() {
    return errorType == ErrorType.HASH_MISMATCH;
  }
}
