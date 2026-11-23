import type { ISetting } from "../../../metadata-utils/src";
import type { TableRow } from "../../../metadata-utils/src/types";
import type { TablePermission } from "../../../tailwind-components/types/types";

export type User = {
  email: string;
  enabled: boolean;
  roles?: string[];
  tokens?: string[];
} & TableRow;

export interface BackendUser {
  email: string;
  settings: ISetting[];
  enabled: boolean;
  roles?: Role[];
}

export interface SchemaInfo {
  id: string;
  label: string;
}

export interface Role {
  schemaId: string;
  role: string;
}

export interface UpdateUser {
  email: string;
  enabled?: boolean;
  password?: string;
  roles?: Role[];
  revokedRoles: Role[];
}

type abstractRole = { schemaId: string; role: string; users: string[] };

export type BackendCustomRole = abstractRole & {
  permissions: TablePermission[];
};

export type CustomRole = abstractRole & TableRow;
