<template>
  <div id="app">
    <Molgenis :menuItems="menuItems" v-model="session">
      <Groups :session="session" :key="JSON.stringify(session)" />
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
        { label: "Databases", href: ".", active: true },
        {
          label: "GraphQL API",
          href: "/apps/graphql-playground/",
        },
        {
          label: "Styleguide (for developers)",
          href: "/apps/styleguide/",
        },
        // remove docs until available
        // {
        //   label: "Docs",
        //   href: "/apps/docs/",
        //   newWindow: true,
        // },
      ],
    };
  },
};
</script>
