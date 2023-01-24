import { ISetting } from "../../Interfaces/ISetting";

export interface ISession {
  email?: string;
  locale?: string;
  roles?: string[];
  schemas?: any;
  settings?: Record<string, string>;
  manifest?: IManifest;
  token?: string;
}

export interface IResponse {
  status: string;
  value: any;
  reason: IErrorMessage;
  _settings: ISetting[];
  _manifest: IManifest;
  _session: ISession;
}

export interface IErrorMessage {
  response?: { data?: { errors: { message: string }[] } };
}

interface IManifest {
  ImplementationVersion: string;
  SpecificationVersion: string;
  DatabaseVersion: string;
}
