import { reactive, ref } from "vue";
import type { User } from "../interfaces/interfaces";
import { createUser, deleteUser, getUsers } from "../util/adminUtils";
import type { ITableSettings } from "../../../tailwind-components/types/types";


export default function useAdminSettings() {
  const users = ref<User[]>([]);
  const userCount = ref(0);
  const totalUserPages = ref(0);

  retrieveUsers();

  async function retrieveUsers() {
    const { newUsers, newUserCount } = await getUsers();
    users.value = newUsers;
    userCount.value = newUserCount;
  // const offset = (settings.page - 1) * settings.pageSize;
    // const divided = userCount.value / settings.pageSize;
    // totalUserPages.value =
    //   userCount.value % settings.pageSize > 0
    //     ? Math.floor(divided) + 1
    //     : divided;
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
