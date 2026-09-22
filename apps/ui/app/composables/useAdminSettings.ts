import { reactive, ref } from "vue";
import {
  createUser,
  deleteUser,
  getUsers,
  type IUser,
} from "../util/adminUtils";

const LIMIT = 100;

const users = ref<IUser[]>([]);
const userCount = ref(0);

export default function useAdminSettings() {
  const totalUserPages = ref(0);

  retrieveUsers();

  async function retrieveUsers(currentPage: number = 1) {
    const { newUsers, newUserCount } = await getUsers();
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

  async function removeUser(user: IUser) {
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
