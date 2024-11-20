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
    label: "Tailwind Components (Beta)",
    href: "/apps/tailwind-components/",
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
      if (this.session && this.session.email == "admin") {
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
    if (resp?._settings[0]?.key === "menu") {
      try {
        this.menu = JSON.parse(resp._settings[0].value);
      } catch (error) {
        console.log("Error parsing menu", error);
      }
    }
  },
};
</script>
