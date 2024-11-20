export interface IKeyValuePair {
  [key: string]: string;
}

export interface ISettings {
  key: string;
  value?: string;
}

export interface ISettingsResponse {
  _settings: ISettings[];
}

export interface ISchema {
  name: string;
}

export interface ISchemaResponse {
  _schema: ISchema;
}

export interface IMgError {
  message: string;
}

export interface IMgErrorResponse {
  response: {
    errors: IMgError[];
  };
}
