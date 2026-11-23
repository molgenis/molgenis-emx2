import { reactive, ref } from "vue";
import type {
  CustomRole,
  CustomRoleUpdate,
  NewCustomRole,
  Role,
  RolePermission,
  SchemaInfo,
  User,
} from "../interfaces/interfaces.ts";
import {
  createUser,
  createCustomRole,
  updateCustomRole,
  deleteCustomRoles,
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
const customRolePermissions = ref<Record<string, RolePermission[]>>({});

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
    }
    return customRoles.value;
  }

  function reloadCustomRoles() {
    customRoles.value = undefined;
    return getCustomRoles();
  }

  async function addCustomRole(newRole: NewCustomRole) {
    await createCustomRole(newRole);
    await Promise.all([reloadCustomRoles(), reloadAdminData()]);
  }

  async function editCustomRole(roleUpdate: CustomRoleUpdate) {
    await updateCustomRole(roleUpdate);
    await Promise.all([reloadCustomRoles(), reloadAdminData()]);
  }

  async function removeCustomRoles(rolesToRemove: CustomRole[]) {
    await deleteCustomRoles(rolesToRemove);
    await Promise.all([reloadCustomRoles(), reloadAdminData()]);
  }

  function getPermissionsForCustomRole(role: CustomRole): RolePermission[] {
    return customRolePermissions.value[role._rowIdString] || [];
  }

  return reactive({
    roles,
    rolesByUser,
    schemas,
    users,
    customRoles,
    totalUserPages,
    addUser,
    getCustomRoles,
    reloadCustomRoles,
    addCustomRole,
    editCustomRole,
    removeCustomRoles,
    getPermissionsForCustomRole,
    getRolesForUser,
    reloadAdminData,
    removeUsers,
  });
}
