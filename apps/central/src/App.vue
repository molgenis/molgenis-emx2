<template>
  <div id="app">
    <Molgenis :menuItems="menuItems" v-model="session">
      <RouterView :session="session" :key="JSON.stringify(session)" />
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "molgenis-components";
import { request, gql } from "graphql-request";
const defaultMenuItems = [
  { label: "Databases", href: "/apps/central/", active: true },
  {
    label: "GraphQL API",
    href: "/apps/graphql-playground/",
  },
  {
    label: "Components (for developers)",
    href: "/apps/molgenis-components/",
  },
  {
    label: "New Components (Beta)",
    href: "/apps/tailwind-components/",
  },
  {
    label: "New UI (Alpha)",
    href: "/apps/ui/",
  },
  {
    label: "Help",
    href: "/apps/docs/",
  },
];
export default {
  components: {
    Molgenis,
  },
  data: function () {
    return {
      session: {},
      menu: defaultMenuItems,
    };
  },
  computed: {
    menuItems() {
      if (this.session && this.session.admin) {
        return [
          ...this.menu,
          { label: "Admin", href: "/apps/central/#/admin", role: "Admin" },
        ];
      } else {
        return this.menu;
      }
    },
  },
  async created() {
    const resp = await request(
      "graphql",
      gql`
        query CentralMenuQuery {
          _settings(keys: ["menu"]) {
            key
            value
          }
        }
      `
    );
    if (resp?._settings.map((s) => s.key).includes("menu")) {
      try {
        this.menu = JSON.parse(
          resp._settings.find((s) => s.key === "menu").value
        );
      } catch (error) {
        console.log("Error parsing menu", error);
      }
    }
  },
};
</script>
