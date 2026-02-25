// AUTO-GENERATED from protocol/hpc-protocol.json — do not edit

export const API_VERSION = "2025-01";

export const JOB_STATUSES = ["PENDING", "CLAIMED", "SUBMITTED", "STARTED", "COMPLETED", "FAILED", "CANCELLED"];

export const TERMINAL_STATUSES = new Set(["COMPLETED", "FAILED", "CANCELLED"]);

export const ARTIFACT_STATUSES = ["CREATED", "UPLOADING", "REGISTERED", "COMMITTED", "FAILED"];

export const ARTIFACT_RESIDENCES = ["managed", "posix", "s3", "http", "reference"];

export function isTerminal(status) {
  return TERMINAL_STATUSES.has(status);
}
