import { reactive, ref } from "vue";
import type { TablePermission } from "../../../tailwind-components/types/types.ts";
import type {
  CustomRole,
  Role,
  SchemaInfo,
  User,
} from "../interfaces/interfaces.ts";
import {
  createUser,
  deleteUser,
  getRoles,
  getSchemas,
  getUsers,
  loadCustomRoles,
} from "../util/adminUtils";

const roles = ref<string[]>([]);
const rolesByUser = ref<Record<string, Role[]>>({});
const schemas = ref<SchemaInfo[]>([]);
const totalUserPages = ref(0);
const users = ref<User[]>([]);
const loaded = ref(false);
const customRoles = ref<CustomRole[]>();
const customRolePermissions = ref<Record<string, TablePermission[]>>({});

export default async function useAdminSettings() {
  await loadAdminData();

  async function loadAdminData() {
    if (loaded.value) {
      return;
    }

    const { newUsers, newRolesByUser } = await getUsers();
    users.value = newUsers;
    rolesByUser.value = newRolesByUser;

    schemas.value = await getSchemas();
    // TODO get the correct roles, this does not retrieve the all custom roles for each schema
    roles.value = await getRoles(schemas.value);
    loaded.value = true;
  }

  function reloadAdminData() {
    loaded.value = false;
    return loadAdminData();
  }

  async function addUser(userName: string, password: string) {
    await createUser(userName, password);
    await reloadAdminData();
  }

  async function removeUsers(users: User[]) {
    await Promise.all(users.map((user) => deleteUser(user)));
    await reloadAdminData();
  }

  function getRolesForUser(email: string): Role[] {
    return rolesByUser.value[email] || [];
  }

  async function getCustomRoles() {
    if (!customRoles.value) {
      const { customRoles: newRoles, permissions: newPermissions } =
        await loadCustomRoles();
      customRoles.value = newRoles;
      customRolePermissions.value = newPermissions;
      console.log("customRoles.value", customRoles.value);
    }
    return customRoles.value;
  }

  function getPermissionsForCustomRole(role: CustomRole): TablePermission[] {
    return customRolePermissions.value[role._rowIdString] || [];
  }

  return reactive({
    roles,
    rolesByUser,
    schemas,
    users,
    totalUserPages,
    addUser,
    getCustomRoles,
    getPermissionsForCustomRole,
    getRolesForUser,
    reloadAdminData,
    removeUsers,
  });
}
