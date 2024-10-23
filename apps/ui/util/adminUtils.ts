import type { ISetting } from "../../metadata-utils/dist";
const GRAPHQL = "/graphql";
const API_GRAPHQL = "/api/graphql";

export function deleteUser(user: IUser) {
  useFetch(API_GRAPHQL, {
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
  return useFetch(GRAPHQL, {
    method: "post",
    body: {
      query: `mutation updateUser($updateUser:updateUser){message}`,
      updateUser: updateUser,
    },
  }).catch((error) => {
    handleError("Error updating user: ", error.value);
  });
}

function createUpdateUser(user: IUser) {
  let updateUser: {
    email: string;
    enabled: boolean;
    password?: string;
    roles?: IRole[];
  } = { email: user.email, enabled: user.enabled };
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

  return useFetch(GRAPHQL, {
    method: "post",
    body: {
      query: `mutation{changePassword(email: "${newUserName}", password: "${newPassword}"){status,message}}`,
    },
  }).catch((error) => {
    handleError("Error creating/updating user: ", error.value);
  });
}

export async function getRoles(schemas: ISchemaInfo[]) {
  if (!schemas.length) return [];

  const { data, error } = await useFetch<{
    data: { _schema: { roles: { name: string }[] } };
  }>("../" + schemas[0].id + GRAPHQL, {
    method: "post",
    body: { query: "{_schema{roles{name}}}" },
  });

  if (error.value) {
    handleError("Error getting roles: ", error);
  }

  return (
    data.value?.data._schema.roles.map((role: { name: string }) => role.name) ||
    []
  );
}

export async function getSchemas() {
  const { data } = await useFetch<{ data: { _schemas: ISchemaInfo[] } }>(
    GRAPHQL,
    {
      method: "post",
      body: {
        query: "{_schemas{id,label}}",
      },
    }
  );
  return data.value?.data._schemas || [];
}

export async function getUsers() {
  const { data, error } = await useFetch<IAdminResponse>(API_GRAPHQL, {
    method: "post",
    body: {
      query: `{ _admin { users { email, settings, {key, value}, enabled, roles { schemaId, role } } userCount } }`,
    },
  });

  if (error.value) {
    handleError("Error loading users: ", error.value);
  }

  const newUsers = buildUsers(data.value?.data._admin.users || []);
  const newUserCount = data.value?.data._admin.userCount ?? 0;
  return { newUsers, newUserCount };
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

export interface IUser {
  //TODO split into communication and internal interface
  email: string;
  settings: ISetting[];
  enabled: boolean;
  tokens?: string[];
  roles?: IRole[];
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
