import type { ISetting } from "../../metadata-utils/dist";
const GRAPHQL = "/graphql";
const API_GRAPHQL = "/api/graphql";

export async function deleteUser(user: IUser) {
  $fetch(API_GRAPHQL, {
    method: "post",
    body: {
      query: `mutation{removeUser(email: "${user.email}"){status,message}}`,
      member: user.email,
    },
  }).catch((error) => {
    handleError("Error deleting user: ", error.value);
  });
}

export async function updateUser(user: IUser) {
  const updateUser = createUpdateUser(user);
  return $fetch(GRAPHQL, {
    method: "post",
    body: {
      query: `mutation updateUser($updateUser:InputUpdateUser) {updateUser(updateUser:$updateUser){status, message}}`,
      variables: { updateUser },
    },
  }).catch((error) => {
    handleError("Error updating user: ", error.value);
  });
}

function createUpdateUser(user: IUser) {
  let updateUser: IUpdateUser = {
    email: user.email,
    enabled: user.enabled,
    revokedRoles: user.revokedRoles || [],
  };
  if (user.password) {
    updateUser.password = user.password;
  }
  if (user.roles) {
    updateUser.roles = user.roles;
  }
  return updateUser;
}

export function createUser(newUserName: string, newPassword: string) {
  if (!newUserName || !newPassword) return;

  return $fetch(GRAPHQL, {
    method: "post",
    body: {
      query: `mutation{changePassword(email: "${newUserName}", password: "${newPassword}"){status,message}}`,
    },
  }).catch((error) => {
    handleError("Error creating/updating user: ", error.value);
  });
}

export async function getRoles(schemas: ISchemaInfo[]): Promise<string[]> {
  if (!schemas.length) return [];

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

export function getSchemas() {
  return $fetch<{ data: { _schemas: ISchemaInfo[] } }>(GRAPHQL, {
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

export async function getUsers() {
  return $fetch<IAdminResponse>(API_GRAPHQL, {
    method: "post",
    body: {
      query: `{ _admin { users { email, settings, {key, value}, enabled, roles { schemaId, role } } userCount } }`,
    },
  })
    .then((response) => {
      const users = buildUsers(response?.data._admin.users || []);
      const userCount = response?.data._admin.userCount ?? 0;
      return { newUsers: users, newUserCount: userCount };
    })
    .catch((error) => {
      handleError("Error loading users: ", error.value);
      return { newUsers: [], newUserCount: 0 };
    });
}

function buildUsers(dataUsers: IUser[]) {
  return dataUsers.map((user) => {
    return { ...user, tokens: getTokens(user) };
  });
}

function getTokens(user: IUser) {
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

interface IAdminResponse {
  data: {
    _admin: {
      users: IUser[];
      userCount: number;
    };
  };
}

export interface ISchemaInfo {
  id: string;
  label: string;
}

export interface IRole {
  schemaId: string;
  role: string;
}

interface IUpdateUser {
  email: string;
  enabled: boolean;
  password?: string;
  roles?: IRole[];
  revokedRoles: IRole[];
}
