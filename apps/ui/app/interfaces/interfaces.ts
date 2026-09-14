import type { ISetting } from "../../../metadata-utils/src";

export interface IUser {
  //TODO split into communication and internal interface
  email: string;
  settings: ISetting[];
  enabled: boolean;
  tokens?: string[];
  roles?: IRole[];
  revokedRoles?: IRole[];
  password?: string;
}

export interface ISchemaInfo {
  id: string;
  label: string;
}

export interface IRole {
  schemaId: string;
  role: string;
}
