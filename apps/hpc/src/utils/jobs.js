export const JOB_STATUSES = [
  "PENDING",
  "CLAIMED",
  "SUBMITTED",
  "STARTED",
  "COMPLETED",
  "FAILED",
  "CANCELLED",
];

export const TERMINAL_STATUSES = new Set(["COMPLETED", "FAILED", "CANCELLED"]);

export function isTerminal(status) {
  return TERMINAL_STATUSES.has(status);
}

export function formatDate(val) {
  if (!val) return "-";
  try {
    return new Date(val).toLocaleString();
  } catch {
    return val;
  }
}
