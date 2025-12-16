// UNKNOWN -> initial/no known data
// RUNNING -> process ongoing
// DONE -> process finished
// INVALID -> process finished (no error): input/output validation failed
// ERROR -> process did not finish correctly (f.e. non-200 HTTP code or non-0 exit code)
export type ProcessStatus =
  | "UNKNOWN"
  | "RUNNING"
  | "DONE"
  | "INVALID"
  | "ERROR";

export interface ProcessData {
  status: ProcessStatus;
  output?: string;
  error?: string;
}
