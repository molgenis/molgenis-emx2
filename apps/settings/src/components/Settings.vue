<template>
  <Molgenis title="Settings" :menuItems="menuItems" v-model="session">
    <Members :key="session.email" :session="session" />
    <Layout :key="session.email" :session="session" />
  </Molgenis>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";
import Members from "./Members";
import Layout from "./Layout";

export default {
  components: {
    Members,
    Layout,
    Molgenis
  },
  data() {
    return {
      session: {}
    };
  },
  computed: {
    menuItems() {
      return [
        { label: "Tables", href: "../tables/" },
        {
          label: "Schema",
          href: "../schema/"
        },
        {
          label: "Upload",
          href: "../import/"
        },
        {
          label: "Download",
          href: "../download/"
        },
        {
          label: "GraphQL",
          href: "/api/playground.html?schema=/api/graphql/" + this.schema
        },
        {
          label: "Settings",
          href: "../settings/"
        }
      ];
    }
  }
};
</script>
