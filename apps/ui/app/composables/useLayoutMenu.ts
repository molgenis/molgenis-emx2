import { computed, type Ref } from "vue";
import { useMenu } from "../../../tailwind-components/app/composables/useMenu";
import type {
  ISession,
  MenuItem,
} from "../../../tailwind-components/types/types";

const DEFAULT_MAIN_MENU: MenuItem[] = [
  { label: "Databases", link: "/", isSpaLink: true },
  { label: "API", link: "apps/graphql-playground", isSpaLink: false },
  {
    label: "Components (for developers)",
    link: "apps/tailwind-components",
    isSpaLink: false,
  },
  { label: "Help", link: "apps/docs", isSpaLink: false },
  { label: "Classic UI", link: "apps/central/", isSpaLink: false },
  { label: "Admin", link: "/admin", isSpaLink: true, role: "Admin" },
];

export async function useLayoutMenu(
  schema: Ref<string>,
  session: Ref<ISession | null>
) {
  const isAdmin = computed(() => session.value?.admin);
  const menu = await useMenu();

  const defaultSchemaMenu = computed<MenuItem[]>(() => [
    { label: "Tables", link: "", isSpaLink: true },
    {
      label: "Schema",
      link: `${schema.value}/schema`,
      isSpaLink: false,
      role: "Viewer",
    },
    {
      label: "SHACL",
      link: `shacl`,
      isSpaLink: true,
      role: "Viewer",
    },
    {
      label: "Up/Download",
      link: `${schema.value}/updownload`,
      isSpaLink: false,
      role: "Viewer",
    },
    {
      label: "Reports",
      link: `${schema.value}/reports`,
      isSpaLink: false,
      role: "Viewer",
    },
    {
      label: "Jobs & Scripts",
      link: `${schema.value}/tasks`,
      isSpaLink: false,
      role: "Manager",
    },
    {
      label: "Settings",
      link: `${schema.value}/settings`,
      isSpaLink: false,
      role: "Manager",
    },
    {
      label: "GraphQL",
      link: `${schema.value}/graphql-playground`,
      isSpaLink: false,
    },
    {
      label: "Analytics",
      link: `analytics`,
      isSpaLink: true,
      role: "Manager",
    },
    {
      label: "Pages",
      link: `pages`,
      isSpaLink: true,
      role: "Manager",
    },
    {
      label: "Help",
      link: `${schema.value}/docs`,
      isSpaLink: false,
    },
  ]);

  const menuItems = computed<MenuItem[]>(() => {
    if (menu.value.length === 0) {
      return schema.value ? defaultSchemaMenu.value : DEFAULT_MAIN_MENU;
    }

    return menu.value.map((item) => {
      return {
        label: item.label,
        link: item.link,
        isSpaLink: typeof item.isSpaLink === "boolean" ? item.isSpaLink : false,
        role: item.role,
        submenu: item.submenu,
      };
    });
  });

  const userMenuItems = computed(() => {
    if (isAdmin.value === true) {
      return menuItems.value;
    }

    const isVisibleToUser = (item: MenuItem): boolean => {
      if (item.role) {
        if (session.value?.roles && Array.isArray(session.value.roles)) {
          return session.value.roles.includes(item.role);
        } else {
          return item.role === "Admin" && isAdmin.value === true;
        }
      }
      return true;
    };

    const removeInvisibleSubmenus = (items: MenuItem[]): MenuItem[] => {
      return items
        .filter(isVisibleToUser)
        .map((item) => {
          if (item.submenu && item.submenu.length > 0) {
            return { ...item, submenu: removeInvisibleSubmenus(item.submenu) };
          }
          return item;
        })
        .filter((item) => !item.submenu || item.submenu.length > 0);
    };

    const visibleMenuItems = menuItems.value.filter(isVisibleToUser);
    return removeInvisibleSubmenus(visibleMenuItems);
  });

  return {
    menuItems,
    userMenuItems,
  };
}
