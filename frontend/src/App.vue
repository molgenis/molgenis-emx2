<template>
    <div id="app">
        <Molgenis
            v-model="session"
            :menu-items="menuItems"
            title="Welcome to MOLGENIS"
        >
            <Groups :key="JSON.stringify(session)" :session="session" />
        </Molgenis>
    </div>
</template>

<script>
import Groups from "./components/central/Groups.vue";
import { Molgenis } from "./components/ui/layout/Molgenis.vue";

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
