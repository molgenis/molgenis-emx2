import { $fetch } from "ofetch";

export interface IColumnAccess {
  editable: string[] | null;
  readonly: string[] | null;
  hidden: string[] | null;
}

export interface IPermission {
  table: string;
  select: string | null;
  insert: string | null;
  update: string | null;
  delete: string | null;
  grant: boolean | null;
  columns: IColumnAccess | null;
}

export interface IRoleInfo {
  name: string;
  description: string | null;
  system: boolean;
  permissions: IPermission[];
}

export interface ITableInfo {
  id: string;
  label: string;
  tableType: string;
}

export const SELECT_OPTIONS = [
  "EXISTS",
  "RANGE",
  "AGGREGATOR",
  "COUNT",
  "TABLE",
  "ROW",
];
export const MODIFY_OPTIONS = ["TABLE", "ROW"];

export async function getRolesAndTables(
  schemaId: string
): Promise<{ roles: IRoleInfo[]; tables: ITableInfo[] }> {
  const response = await $fetch<{
    data: { _schema: { roles: IRoleInfo[]; tables: ITableInfo[] } };
  }>(`/${schemaId}/graphql`, {
    method: "POST",
    body: {
      query: `{_schema{roles{name,description,system,permissions{table,select,insert,update,delete,grant,columns{editable,readonly,hidden}}},tables{id,label,tableType}}}`,
    },
  });
  return {
    roles: response.data._schema.roles || [],
    tables: response.data._schema.tables || [],
  };
}

export async function saveRole(
  schemaId: string,
  roleName: string,
  description: string | null,
  permissions: IPermission[]
): Promise<string> {
  const response = await $fetch<{ data: { change: { message: string } } }>(
    `/${schemaId}/graphql`,
    {
      method: "POST",
      body: {
        query: `mutation change($roles:[MolgenisRoleInput]){change(roles:$roles){message}}`,
        variables: {
          roles: [
            {
              name: roleName,
              description,
              permissions: permissions.map((perm) => ({
                table: perm.table,
                select: perm.select,
                insert: perm.insert,
                update: perm.update,
                delete: perm.delete,
                grant: perm.grant,
              })),
            },
          ],
        },
      },
    }
  );
  return response.data.change.message;
}

export async function createRole(
  schemaId: string,
  roleName: string,
  description: string | null
): Promise<string> {
  const response = await $fetch<{ data: { change: { message: string } } }>(
    `/${schemaId}/graphql`,
    {
      method: "POST",
      body: {
        query: `mutation change($roles:[MolgenisRoleInput]){change(roles:$roles){message}}`,
        variables: {
          roles: [{ name: roleName, description }],
        },
      },
    }
  );
  return response.data.change.message;
}

export async function deleteRole(
  schemaId: string,
  roleName: string
): Promise<string> {
  const response = await $fetch<{ data: { drop: { message: string } } }>(
    `/${schemaId}/graphql`,
    {
      method: "POST",
      body: {
        query: `mutation drop($roles:[String]){drop(roles:$roles){message}}`,
        variables: { roles: [roleName] },
      },
    }
  );
  return response.data.drop.message;
}

export async function dropPermission(
  schemaId: string,
  roleName: string,
  tableName: string
): Promise<string> {
  const response = await $fetch<{ data: { drop: { message: string } } }>(
    `/${schemaId}/graphql`,
    {
      method: "POST",
      body: {
        query: `mutation drop($permissions:[MolgenisPermissionDropInput]){drop(permissions:$permissions){message}}`,
        variables: { permissions: [{ role: roleName, table: tableName }] },
      },
    }
  );
  return response.data.drop.message;
}

export function getDataTables(tables: ITableInfo[]): ITableInfo[] {
  return tables
    .filter((table) => table.tableType === "DATA")
    .sort((a, b) => a.label.localeCompare(b.label));
}
