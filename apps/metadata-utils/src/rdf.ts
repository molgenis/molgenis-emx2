export interface ShaclSet {
  id: string;
  name: string;
  version: string;
  sources: string[];
}

export interface ShaclSetValidation extends ShaclSet {
  status: ShaclStatus;
  output: string;
  error: string;
  isViewed: boolean;
}

export type ShaclStatus = "UNKNOWN" | "RUNNING" | "VALID" | "INVALID" | "ERROR";