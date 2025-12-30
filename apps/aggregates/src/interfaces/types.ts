export interface selectedFiltersIF {
  researchCenter: string[];
  primaryTumorSite: string[];
  metastasis: string[];
  yearOfDiagnosis: string[];
  sex: string[];
}

export interface researchCentersIF {
  researchCenter: string;
  _sum: number;
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

export interface chartAxisSettingsIF {
  ticks: string[] | number[];
  ymax: number | null;
}

export interface nestedSelectedFiltersQueryIF {
  name?: {
    equals: string;
  };
  id?: {
    equals: string;
  };
}

export interface selectedFiltersQueryIF {
  researchCenter?: nestedSelectedFiltersQueryIF;
  primaryTumorSite?: nestedSelectedFiltersQueryIF;
  metastatis?: nestedSelectedFiltersQueryIF;
  yearOfDiagnosis?: nestedSelectedFiltersQueryIF;
  sex?: nestedSelectedFiltersQueryIF;
}

export interface vizChartFilters extends selectedFiltersQueryIF {
  _or?: selectedFiltersQueryIF[];
}
