import { ISetting } from "metadata-utils";

export interface IRolePermission {
  table: string;
  select?: string;
  insert?: boolean;
  update?: boolean;
  delete?: boolean;
}

export interface IRole {
  name: string;
  description?: string;
  system: boolean;
  permissions: IRolePermission[];
}

export interface ISession {
  email?: string;
  locale?: string;
  roles?: IRole[];
  schemas?: any;
  settings?: Record<string, string | number | boolean>;
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
