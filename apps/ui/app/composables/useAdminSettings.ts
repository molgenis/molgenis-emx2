import { reactive, ref } from "vue";
import type { User } from "../interfaces/interfaces";
import { createUser, deleteUser, getUsers } from "../util/adminUtils";

export default function useAdminSettings() {
  const users = ref<User[]>([]);
  const userCount = ref(0);
  const totalUserPages = ref(0);

  retrieveUsers();

  async function retrieveUsers() {
    const { newUsers, newUserCount } = await getUsers();
    users.value = newUsers;
    userCount.value = newUserCount;
  }

  async function addUser(userName: string, password: string) {
    await createUser(userName, password);
    retrieveUsers();
  }

  async function removeUsers(users: User[]) {
    await Promise.all(users.map((user) => deleteUser(user)));
    await retrieveUsers();
  }

  return reactive({
    users,
    userCount,
    totalUserPages,
    addUser,
    removeUsers,
    retrieveUsers,
  });
}
