export interface IKeyValue {
  key: string;
  value?: string;
}

export interface IDatasetApiResponse {
  Dataset: IDatasetSchema[];
}

export interface ISettingsApiResponse {
  _settings: IKeyValue[];
}

export interface IDistributionFilesSchema {
  identifier: string;
  format?: {
    name: string;
  };
  name?: string;
}

export interface IDatasetDistributionSchema {
  name: string;
  description?: string;
  type: {
    name: string;
  };
  files?: IDistributionFilesSchema[];
}

export interface IDatasetSchema {
  id: string;
  title: string;
  description: string;
  distribution: IDatasetDistributionSchema[];
}

export interface IDatasetRef {
  id: string;
  title: string;
  description: string;
  fileCount: number;
  fileFormats: string[];
  type: string;
}
