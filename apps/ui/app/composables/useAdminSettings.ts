import { reactive, ref } from "vue";
import type { User } from "../interfaces/interfaces";
import { createUser, deleteUser, getUsers } from "../util/adminUtils";

const LIMIT = 100;

export default function useAdminSettings() {
  const users = ref<User[]>([]);
  const userCount = ref(0);
  const totalUserPages = ref(0);

  retrieveUsers();

  async function retrieveUsers(currentPage: number = 1) {
    const offset = (currentPage - 1) * LIMIT;
    const { newUsers, newUserCount } = await getUsers(offset, LIMIT);
    users.value = newUsers;
    userCount.value = newUserCount;
    const divided = userCount.value / LIMIT;
    totalUserPages.value =
      userCount.value % LIMIT > 0 ? Math.floor(divided) + 1 : divided;
  }

  async function addUser(userName: string, password: string) {
    await createUser(userName, password);
    retrieveUsers();
  }

  async function removeUser(user: User) {
    await deleteUser(user);
    await retrieveUsers();
  }

  return reactive({
    users,
    userCount,
    totalUserPages,
    addUser,
    removeUser,
    retrieveUsers,
  });
}
