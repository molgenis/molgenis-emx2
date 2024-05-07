export interface selectedFiltersIF {
  resource: string[];
  researchCenter: string[];
  primaryTumorSite: string[];
  metastasis: string[];
  yearOfDiagnosis: string[];
  sex: string[];
}

export interface researchCentersIF {
  researchCenter: { name: string };
  _sum: { n: number };
}

export interface primaryTumorSiteIF {
  primaryTumorSite: { name: string };
  _sum: { n: number };
}

export interface metastasisIF {
  metastasis: { name: string };
  _sum: { n: number };
}

export interface yearOfDiagnosisIF {
  yearOfDiagnosis: { name: string };
  _sum: { n: number };
}

export interface sexCasesIF {
  sex: { name: string };
  _sum: { n: number };
}
