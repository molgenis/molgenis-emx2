export interface filterDataIF {
  name: string;
}

export interface BeaconQueryIF {
  query: {
    filters: {
      id: string;
      value: string | string[];
      operator?: string;
    };
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
