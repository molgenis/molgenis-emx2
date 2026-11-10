import { reactive, ref } from "vue";
import type { Role, SchemaInfo, User } from "~/interfaces/interfaces.ts";
import {
  createUser,
  deleteUser,
  getRoles,
  getSchemas,
  getUsers,
} from "../util/adminUtils";

const roles = ref<string[]>([]);
const rolesByUser = ref<Record<string, Role[]>>({});
const schemas = ref<SchemaInfo[]>([]);
const totalUserPages = ref(0);
const users = ref<User[]>([]);
const userCount = ref(0);
const loaded = ref(false);

export default async function useAdminSettings() {
  await loadAdminData();

  async function loadAdminData() {
    if (loaded.value) {
      return;
    }

    const { newUsers, newUserCount, newRolesByUser } = await getUsers();
    users.value = newUsers;
    userCount.value = newUserCount;
    rolesByUser.value = newRolesByUser;

    schemas.value = await getSchemas();
    // TODO get the correct roles, this does not the all custom roles
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

  return reactive({
    roles,
    rolesByUser,
    schemas,
    users,
    userCount,
    totalUserPages,
    addUser,
    getRolesForUser,
    reloadAdminData,
    removeUsers,
  });
}
