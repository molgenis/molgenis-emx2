<template>
  <div id="app">
    <Molgenis :menuItems="menuItems" v-model="session">
      <RouterView :session="session" :key="JSON.stringify(session)" />
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "molgenis-components";

export default {
  components: {
    Molgenis,
  },
  data: function () {
    return {
      session: {},
    };
  },
  computed: {
    menuItems() {
      let result = [
        { label: "Databases", href: "/apps/central/", active: true },
        {
          label: "GraphQL API",
          href: "/apps/graphql-playground/",
        },
        {
          label: "Styleguide (for developers)",
          href: "/apps/styleguide/",
        },
        {
          label: "Help",
          href: "/apps/docs/",
        },
      ];
      if (this.session && this.session.email == "admin") {
        result.push({
          label: "Admin",
          href: "/apps/central/#/admin",
        });
      }
      return result;
    },
  },
};
</script>
