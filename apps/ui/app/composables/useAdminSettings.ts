import { reactive, ref } from "vue";
import type { User } from "../interfaces/interfaces";
import { createUser, deleteUser, getUsers } from "../util/adminUtils";
import type { ITableSettings } from "../../../tailwind-components/types/types";

const LIMIT = 50;
const DEFAULT_TABLE_SETTINGS: ITableSettings = {
  page: 1,
  pageSize: LIMIT,
  orderby: {
    column: "email",
    direction: "ASC",
  },
};

export default function useAdminSettings() {
  const users = ref<User[]>([]);
  const userCount = ref(0);
  const totalUserPages = ref(0);
  const cachedSettings = ref<ITableSettings>(DEFAULT_TABLE_SETTINGS);

  retrieveUsers(cachedSettings.value);

  async function retrieveUsers(settings: ITableSettings) {
    cachedSettings.value = settings;
    const { newUsers, newUserCount } = await getUsers(settings.pageSize);
    users.value = newUsers;
    userCount.value = newUserCount;
    const divided = userCount.value / settings.pageSize;
    totalUserPages.value =
      userCount.value % settings.pageSize > 0
        ? Math.floor(divided) + 1
        : divided;
  }

  async function addUser(userName: string, password: string) {
    await createUser(userName, password);
    retrieveUsers(cachedSettings.value);
  }

  async function removeUser(user: User) {
    await deleteUser(user);
    await retrieveUsers(cachedSettings.value);
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
