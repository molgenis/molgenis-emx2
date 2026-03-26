import { ISetting } from "metadata-utils";

export interface ITablePermission {
  name: string;
  canView: boolean;
  canInsert: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}

export interface ISession {
  email?: string;
  locale?: string;
  roles?: string[];
  tablePermissions?: ITablePermission[];
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
