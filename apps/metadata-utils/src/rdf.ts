import type {ProcessStatus} from "./generic";

export interface ShaclSet {
  id: string;
  name: string;
  version: string;
  sources: string[];
}

export interface ShaclSetValidation extends ShaclSet {
  status: ProcessStatus;
  output: string;
  error: string;
  isViewed: boolean;
}