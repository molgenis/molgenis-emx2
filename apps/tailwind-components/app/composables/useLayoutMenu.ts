import { computed, type Ref } from "vue";
import { useMenu } from "../composables/useMenu";
import type { ISession, MenuItem } from "../../types/types";
import { resolveLinkType } from "../utils/typeUtils";

const DEFAULT_MAIN_MENU: MenuItem[] = [
  { label: "Databases", location: "/" },
  {
    label: "API",
    location: "apps/graphql-playground",
    linkType: "internalPageLink",
  },
  {
    label: "Components (for developers)",
    location: "apps/tailwind-components",
    linkType: "internalPageLink",
  },
  { label: "Help", location: "apps/docs", linkType: "internalPageLink" },
  {
    label: "Classic UI",
    location: "apps/central/",
    linkType: "internalPageLink",
  },
  { label: "Admin", location: "/admin", role: "Admin" },
];

export async function useLayoutMenu(
  schema: Ref<string>,
  session: Ref<ISession | null>
) {
  const { menu, mobileMenu } = await useMenu();

  const defaultSchemaMenu = computed<MenuItem[]>(() => [
    { label: "Tables", location: `/${schema.value}` },
    {
      label: "Schema",
      location: `${schema.value}/schema`,
      linkType: "internalPageLink",
      role: "Viewer",
    },
    {
      label: "SHACL",
      location: `shacl`,
      role: "Viewer",
    },
    {
      label: "Up/Download",
      location: `${schema.value}/updownload`,
      linkType: "internalPageLink",
      role: "Viewer",
    },
    {
      label: "Reports",
      location: `${schema.value}/reports`,
      linkType: "internalPageLink",
      role: "Viewer",
    },
    {
      label: "Jobs & Scripts",
      location: `${schema.value}/tasks`,
      linkType: "internalPageLink",
      role: "Manager",
    },
    {
      label: "Settings",
      location: `${schema.value}/settings`,
      linkType: "internalPageLink",
      role: "Manager",
    },
    {
      label: "GraphQL",
      location: `${schema.value}/graphql-playground`,
      linkType: "internalPageLink",
    },
    {
      label: "Analytics",
      location: `analytics`,
      role: "Manager",
    },
    {
      label: "Pages",
      location: `apps/ui/cms/pages`,
      linkType: "internalPageLink",
      role: "Manager",
    },
    {
      label: "Help",
      location: `${schema.value}/docs`,
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
        location: item.location,
        linkType: resolveLinkType(item),
        role: item.role,
        submenu: item.submenu,
      };
    });
  });

  const userMenuItems = computed(() =>
    filterMenuForUser(menuItems.value, session.value)
  );

  const mobileMenuItems = computed(() =>
    filterMenuForUser(
      mobileMenu.value.map((item) => ({
        ...item,
        linkType: resolveLinkType(item),
      })),
      session.value
    )
  );

  return {
    menuItems,
    userMenuItems,
    mobileMenuItems,
  };
}

export function filterMenuForUser(
  items: MenuItem[],
  session: ISession | null
): MenuItem[] {
  const isAdmin = session?.admin === true;
  if (isAdmin) {
    return items;
  }

  const isVisibleToUser = (item: MenuItem): boolean => {
    if (!item.role) {
      return true;
    }
    if (session?.roles && Array.isArray(session.roles)) {
      return session.roles.includes(item.role);
    }
    return false;
  };

  const removeInvisibleItems = (menu: MenuItem[]): MenuItem[] =>
    menu
      .filter(isVisibleToUser)
      .map((item) =>
        item.submenu && item.submenu.length > 0
          ? { ...item, submenu: removeInvisibleItems(item.submenu) }
          : item
      )
      .filter((item) => !item.submenu || item.submenu.length > 0);

  return removeInvisibleItems(items);
}
