export interface IDashboardHighlights {
  Patients: number;
  "Small variants": number;
  "Structural variants": number;
}

export interface ITopGenes {
  gene: string;
  count: number;
}

export interface ITopDiagnoses {
  diagnosis: string;
  count: number;
}

export interface IEnrollment {
  subregistry: string;
  count: number;
}
