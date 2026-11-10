import { reactive, ref } from "vue";
import type { SchemaInfo, User } from "~/interfaces/interfaces.ts";
import {
  createUser,
  deleteUser,
  getRoles,
  getSchemas,
  getUsers,
} from "../util/adminUtils";

export default async function useAdminSettings() {
  const roles = ref<string[]>([]);
  const schemas = ref<SchemaInfo[]>([]);
  const users = ref<User[]>([]);
  const userCount = ref(0);
  const totalUserPages = ref(0);

  await loadAdminData();

  async function loadAdminData() {
    const { newUsers, newUserCount } = await getUsers();
    users.value = newUsers;
    userCount.value = newUserCount;

    schemas.value = await getSchemas();
    roles.value = await getRoles(schemas.value);
  }

  async function addUser(userName: string, password: string) {
    await createUser(userName, password);
    loadAdminData();
  }

  async function removeUsers(users: User[]) {
    await Promise.all(users.map((user) => deleteUser(user)));
    await loadAdminData();
  }

  return reactive({
    roles,
    schemas,
    users,
    userCount,
    totalUserPages,
    addUser,
    removeUsers,
    retrieveUsers: loadAdminData,
  });
}
