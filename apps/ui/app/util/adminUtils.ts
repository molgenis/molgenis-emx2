import { $fetch } from "ofetch";
import type {
  BackendUser,
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
  newUserCount: number;
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
      userCount 
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
      const newUserCount = response?.data._admin.userCount ?? 0;
      const newRolesByUser = loadedUsers.reduce((acc, user) => {
        acc[user.email] = user.roles || [];
        return acc;
      }, {} as Record<string, Role[]>);

      return { newUsers, newUserCount, newRolesByUser };
    })
    .catch((error) => {
      handleError("Error loading users: ", error.value);
      return { newUsers: [], newUserCount: 0, newRolesByUser: {} };
    });
}

function mapBackendUserToUser(dataUsers: BackendUser[]): User[] {
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

interface AdminResponse {
  data: {
    _admin: {
      users: BackendUser[];
      userCount: number;
    };
  };
}
