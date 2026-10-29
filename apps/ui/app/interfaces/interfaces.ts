import type { ISetting } from "../../../metadata-utils/src";

export interface User {
  //TODO split into communication and internal interface
  email: string;
  settings: ISetting[];
  enabled: boolean;
  tokens?: string[];
  roles?: Role[];
  revokedRoles?: Role[];
  password?: string;
}

export interface SchemaInfo {
  id: string;
  label: string;
}

export interface Role {
  schemaId: string;
  role: string;
}
