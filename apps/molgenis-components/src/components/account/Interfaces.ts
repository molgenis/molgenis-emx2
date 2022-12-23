export interface ISession {
  email?: string;
  roles?: string[];
  schemas?: any;
  settings?: Record<string, string>;
  manifest?: IManifest;
  token?: string;
}

export interface IResponse {
  status: string;
  value: any;
  reason: IReason;
  _settings: ISetting[];
  _manifest: IManifest;
  _session: ISession;
}

export interface IReason {
  response?: { data?: { errors: { message: string }[] } };
}

interface IManifest {
  ImplementationVersion: string;
  SpecificationVersion: string;
  DatabaseVersion: string;
}

export interface ISetting {
  key: string;
  value: string;
}
