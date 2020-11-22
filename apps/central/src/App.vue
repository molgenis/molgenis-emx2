<template>
  <div id="app">
    <Molgenis
      title="Welcome to MOLGENIS central"
      :menuItems="menuItems"
      v-model="session"
    >
      <Groups :session="session" :key="session" />
    </Molgenis>
  </div>
</template>

<script>
import Groups from "./components/Groups";
import { Molgenis } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Molgenis,
    Groups,
  },
  data: function () {
    return {
      session: {},
      menuItems: [
        { label: "Central", href: ".", active: true },
        {
          label: "GraphQL API",
          href: "/apps/graphql-playground/",
        },
        {
          label: "Styleguide",
          href: "/apps/styleguide/",
        },
        {
          label: "Docs",
          href: "/apps/docs/",
          newWindow: true,
        },
      ],
    };
  },
};
</script>
