import { $fetch } from "ofetch";
import type { TablePermission } from "../../../tailwind-components/types/types";
import type {
  BackendCustomRole,
  BackendUser,
  CustomRole,
  CustomRoleUpdate,
  NewCustomRole,
  Role,
  SchemaInfo,
  UpdateUser,
  User,
} from "../interfaces/interfaces";

const GRAPHQL = "/graphql";
const API_GRAPHQL = "/api/graphql";

export async function deleteUser(user: User) {
  return $fetch(API_GRAPHQL, {
    method: "post",
    body: {
      query: `mutation{removeUser(email: "${user.email}"){status,message}}`,
      member: user.email,
    },
  }).catch((error) => {
    handleError("Error deleting user: ", error.value);
  });
}

export async function updateUser(user: UpdateUser) {
  return $fetch(API_GRAPHQL, {
    method: "post",
    body: {
      query: `mutation updateUser($updateUser:InputUpdateUser) {updateUser(updateUser:$updateUser){status, message}}`,
      variables: { updateUser: user },
    },
  }).catch((error) => {
    handleError("Error updating user: ", error.value);
  });
}

export async function createUser(newUserName: string, newPassword: string) {
  if (!newUserName || !newPassword) return;

  return $fetch(API_GRAPHQL, {
    method: "post",
    body: {
      query: `mutation{changePassword(email: "${newUserName}", password: "${newPassword}"){status,message}}`,
    },
  }).catch((error) => {
    handleError("Error creating/updating user: ", error);
  });
}

export async function getRoles(schemas: SchemaInfo[]): Promise<string[]> {
  if (!schemas.length || !schemas[0]) return [];

  const gqlUrl = "../../" + schemas[0].id + GRAPHQL;
  return $fetch<{
    data: { _schema: { roles: { name: string }[] } };
  }>(gqlUrl, {
    method: "post",
    body: { query: "{_schema{roles{name}}}" },
  })
    .then((response) => {
      return (
        response.data._schema.roles.map(
          (role: { name: string }) => role.name
        ) || []
      );
    })
    .catch((error) => {
      handleError("Error getting roles: ", error);
      return [];
    });
}

export async function getSchemas() {
  return $fetch<{ data: { _schemas: SchemaInfo[] } }>(API_GRAPHQL, {
    method: "post",
    body: {
      query: "{_schemas{id,label}}",
    },
  })
    .then((response) => {
      return response.data._schemas || [];
    })
    .catch((error) => {
      handleError("Error getting schemas: ", error);
      return [];
    });
}

export async function getUsers(): Promise<{
  newUsers: User[];
  newRolesByUser: Record<string, Role[]>;
}> {
  const query = `
  { _admin 
    { users
      { email,
        settings {key, value}, 
        enabled, 
        roles { schemaId, role } 
      } 
    } 
  }`;
  return $fetch<AdminResponse>(API_GRAPHQL, {
    method: "post",
    body: {
      query,
      variables: {},
    },
  })
    .then((response) => {
      const loadedUsers = response?.data._admin.users || [];
      const newUsers = mapBackendUserToUser(loadedUsers);
      const newRolesByUser = loadedUsers.reduce(
        (acc: Record<string, Role[]>, user) => {
          acc[user.email] = user.roles || [];
          return acc;
        },
        {}
      );

      return { newUsers, newRolesByUser };
    })
    .catch((error) => {
      handleError("Error loading users: ", error.value);
      return { newUsers: [], newRolesByUser: {} };
    });
}

export function mapBackendUserToUser(dataUsers: BackendUser[]): User[] {
  return dataUsers.map((user) => {
    return {
      email: user.email,
      enabled: user.enabled,
      roles: (user.roles ?? []).map(
        (role) => `${role.schemaId} (${role.role})`
      ),
      tokens: getTokens(user),
      _rowId: { email: user.email },
      _rowIdString: user.email,
    };
  });
}

function getTokens(user: BackendUser) {
  if (user.settings.length) {
    const tokens = user.settings.find((setting) => {
      return setting.key === "access-tokens";
    });
    if (tokens) {
      return tokens.value.split(",");
    }
  }
  return [];
}

function handleError(message: string, error: any) {
  console.log(message, error);
  //see nuxt catalogue on how to handle errors
}

export function isValidPassword(password1: string, password2: string) {
  return password1.length > 7 && password1 === password2;
}

export async function loadCustomRoles(): Promise<{
  customRoles: CustomRole[];
  permissions: Record<string, TablePermission[]>;
}> {
  const query = `query customRoles{
    _admin {
      customRoles {
        schemaId, 
        role, 
        users, 
        permissions { 
          table, select, insert, update, delete, isRowLevel 
        }
      }
    }
  }`;
  return $fetch<AdminResponse>(API_GRAPHQL, {
    method: "post",
    body: {
      query,
    },
  })
    .then((response) => {
      return mapBackendCustomRolesToFrontEnd(response.data._admin.customRoles);
    })
    .catch((error) => {
      handleError("Error loading custom roles: ", error.value);
      return { customRoles: [], permissions: {} };
    });
}

export async function deleteCustomRoles(customRoles: CustomRole[]) {
  if (!customRoles.length) return;

  return $fetch(API_GRAPHQL, {
    method: "post",
    body: {
      query: `mutation dropRoles($roles:[DropRoleInput]){drop(roles:$roles){status,message}}`,
      variables: {
        roles: customRoles.map((customRole) => ({
          schemaId: customRole.schemaId,
          roles: customRole.role,
        })),
      },
    },
  });
}

export async function getRootTableNames(schemaId: string): Promise<string[]> {
  const response = await $fetch<{
    data: {
      _schema: { tables: { name: string; inheritName?: string | null }[] };
    };
  }>(schemaGraphqlUrl(schemaId), {
    method: "post",
    body: { query: "{_schema{tables{name,inheritName}}}" },
  });
  return response.data._schema.tables
    .filter((table) => !table.inheritName)
    .map((table) => table.name);
}

export async function createCustomRole(newRole: NewCustomRole) {
  return changeCustomRole(
    newRole.schemaId,
    newRole.role,
    newRole.permissions,
    newRole.users
  );
}

export async function updateCustomRole(roleUpdate: CustomRoleUpdate) {
  await changeCustomRole(
    roleUpdate.schemaId,
    roleUpdate.role,
    roleUpdate.permissions,
    roleUpdate.addedUsers
  );
  if (roleUpdate.removedUsers.length) {
    await $fetch(schemaGraphqlUrl(roleUpdate.schemaId), {
      method: "post",
      body: {
        query: `mutation dropMemberRoles($memberRoles:[MolgenisMembersInput]){drop(memberRoles:$memberRoles){status,message}}`,
        variables: {
          memberRoles: roleUpdate.removedUsers.map((email) => ({
            email,
            role: roleUpdate.role,
          })),
        },
      },
    });
  }
}

function changeCustomRole(
  schemaId: string,
  role: string,
  permissions: TablePermission[],
  users: string[]
) {
  return $fetch(schemaGraphqlUrl(schemaId), {
    method: "post",
    body: {
      query: `mutation changeRole($roles:[MolgenisRoleInput],$members:[MolgenisMembersInput]){change(roles:$roles,members:$members){status,message}}`,
      variables: {
        roles: [{ name: role, permissions }],
        members: users.map((email) => ({ email, role })),
      },
    },
  });
}

function schemaGraphqlUrl(schemaId: string) {
  return `/${encodeURIComponent(schemaId)}${GRAPHQL}`;
}

export function mapBackendCustomRolesToFrontEnd(
  backEndCustomRoles: BackendCustomRole[]
): {
  customRoles: CustomRole[];
  permissions: Record<string, TablePermission[]>;
} {
  const customRoles =
    backEndCustomRoles.map((customRole) => ({
      schemaId: customRole.schemaId,
      role: customRole.role,
      users: customRole.users,
      tables: (customRole.permissions || []).map(
        (permission) => permission.table
      ),
      _rowId: { schemaId: customRole.schemaId, role: customRole.role },
      _rowIdString: `${customRole.schemaId}-${customRole.role}`,
    })) || [];
  const permissions = backEndCustomRoles.reduce(
    (accum: Record<string, TablePermission[]>, customRole) => {
      accum[`${customRole.schemaId}-${customRole.role}`] =
        customRole.permissions || [];
      return accum;
    },
    {}
  );
  return { customRoles, permissions };
}

export function sortRows<T extends object>(
  rows: T[],
  column: string,
  direction: "ASC" | "DESC"
): T[] {
  return [...rows].sort((a, b) => {
    const aValue = JSON.stringify(a[column as keyof T]);
    const bValue = JSON.stringify(b[column as keyof T]);
    if (aValue === undefined || bValue === undefined) {
      return 0;
    }
    return direction === "ASC"
      ? aValue.localeCompare(bValue)
      : bValue.localeCompare(aValue);
  });
}

export function paginateRows<T>(
  rows: T[],
  page: number,
  pageSize: number
): T[] {
  const startIndex = (page - 1) * pageSize;
  return rows.slice(startIndex, startIndex + pageSize);
}

interface AdminResponse {
  data: {
    _admin: {
      users: BackendUser[];
      customRoles: BackendCustomRole[];
    };
  };
}
