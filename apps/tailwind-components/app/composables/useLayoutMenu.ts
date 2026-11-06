import { computed, type Ref } from "vue";
import { useMenu } from "../composables/useMenu";
import type { ISession, MenuItem } from "../../types/types";
import { resolveLinkType } from "../utils/typeUtils";

const DEFAULT_MAIN_MENU: MenuItem[] = [
  { label: "Databases", link: "/" },
  {
    label: "API",
    link: "apps/graphql-playground",
    linkType: "internalPageLink",
  },
  {
    label: "Components (for developers)",
    link: "apps/tailwind-components",
    linkType: "internalPageLink",
  },
  { label: "Help", link: "apps/docs", linkType: "internalPageLink" },
  { label: "Classic UI", link: "apps/central/", linkType: "internalPageLink" },
  { label: "Admin", link: "/admin", role: "Admin" },
];

export async function useLayoutMenu(
  schema: Ref<string>,
  session: Ref<ISession | null>
) {
  const isAdmin = computed(() => session.value?.admin);
  const menu = await useMenu();

  const defaultSchemaMenu = computed<MenuItem[]>(() => [
    { label: "Tables", link: `/${schema.value}` },
    {
      label: "Schema",
      link: `${schema.value}/schema`,
      linkType: "internalPageLink",
      role: "Viewer",
    },
    {
      label: "SHACL",
      link: `shacl`,
      role: "Viewer",
    },
    {
      label: "Up/Download",
      link: `${schema.value}/updownload`,
      linkType: "internalPageLink",
      role: "Viewer",
    },
    {
      label: "Reports",
      link: `${schema.value}/reports`,
      linkType: "internalPageLink",
      role: "Viewer",
    },
    {
      label: "Jobs & Scripts",
      link: `${schema.value}/tasks`,
      linkType: "internalPageLink",
      role: "Manager",
    },
    {
      label: "Settings",
      link: `${schema.value}/settings`,
      linkType: "internalPageLink",
      role: "Manager",
    },
    {
      label: "GraphQL",
      link: `${schema.value}/graphql-playground`,
      linkType: "internalPageLink",
    },
    {
      label: "Analytics",
      link: `analytics`,
      role: "Manager",
    },
    {
      label: "Pages",
      link: `cms/pages`,
      role: "Manager",
    },
    {
      label: "Help",
      link: `${schema.value}/docs`,
      linkType: "internalPageLink",
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
        linkType: resolveLinkType(item),
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
