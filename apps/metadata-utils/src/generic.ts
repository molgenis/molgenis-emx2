export type ProcessStatus = "UNKNOWN" | "RUNNING" | "DONE" | "INVALID" | "ERROR";

export interface ProcessData {
  status: ProcessStatus;
  out?: string;
  err?: string;
}