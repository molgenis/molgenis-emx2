<template>
  <div id="app">
    <Molgenis
      title="Welcome to MOLGENIS central"
      :menuItems="menuItems"
      v-model="account"
    >
      <Groups :account="account" :key="account" />
    </Molgenis>
  </div>
</template>

<script>
import Groups from "./components/Groups";
import { Molgenis } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Molgenis,
    Groups
  },
  data: function() {
    return {
      account: {},
      menuItems: [
        { label: "Central", href: ".", active: true },
        {
          label: "GraphQL API",
          href: "/api/playground.html?schema=/api/graphql"
        },
        {
          label: "Styleguide",
          href: "/apps/styleguide/"
        }
      ]
    };
  }
};
</script>
