export interface filterDataIF {
  name: string;
}

export interface BeaconFilterIF {
  id: string;
  value: string[];
  operator?: string;
}

export interface BeaconQueryIF {
  query: {
    filters: BeaconFilterIF[];
  };
}

export interface ResultSetIF {
  id: string;
  type: string;
  setType: string;
  exists: boolean;
  resultsCount: number;
  results: object[];
}

export interface OntologyDataIF {
  order?: number;
  name: string;
  label?: string;
  parent?: object[];
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: object[];
}

export interface BeaconResponseSummaryIF {
  exists: boolean;
  numTotalResults: number;
}

export interface BeaconOutputIF {
  meta: object;
  responseSummary: BeaconResponseSummaryIF;
  response: {
    resultSets: ResultSetIF[];
  };
}

export interface BeaconResultsIF {
  schema: string;
  table: string;
  status: string;
  count: number;
}

export interface GraphQlResponseIF {
  data: object;
}

export interface ApiResponseIF {
  data: {
    meta: object;
    responseSummary: BeaconResponseSummaryIF;
    response: {
      resultSets: ResultSetIF[];
    };
  };
}
