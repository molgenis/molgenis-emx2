// AUTO-GENERATED from protocol/hpc-protocol.json — do not edit

export const API_VERSION = "2025-01";

export const JOB_STATUSES = [
	"PENDING",
	"CLAIMED",
	"SUBMITTED",
	"STARTED",
	"COMPLETED",
	"FAILED",
	"CANCELLED",
] as const;

export type JobStatus = (typeof JOB_STATUSES)[number];

export const TERMINAL_STATUSES = new Set<JobStatus>([
	"COMPLETED",
	"FAILED",
	"CANCELLED",
]);

export const ARTIFACT_STATUSES = [
	"CREATED",
	"UPLOADING",
	"REGISTERED",
	"COMMITTED",
	"FAILED",
] as const;

export type ArtifactStatus = (typeof ARTIFACT_STATUSES)[number];

export const ARTIFACT_RESIDENCES = [
	"managed",
	"posix",
	"s3",
	"http",
	"reference",
] as const;

export type ArtifactResidence = (typeof ARTIFACT_RESIDENCES)[number];

export function isTerminal(status: string): boolean {
	return TERMINAL_STATUSES.has(status as JobStatus);
}
