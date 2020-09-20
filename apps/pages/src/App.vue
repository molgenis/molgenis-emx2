<template>
  <Molgenis :menuItems="menuItems" id="__top" v-model="session">
    <router-view :session="session" :page="page" />
  </Molgenis>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Molgenis
  },
  data() {
    return {
      session: null,
      page: null
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
          href: "/api/playground.html?schema=/api/graphql/" + this.schemaName
        },
        {
          label: "Settings",
          href: "../settings/"
        }
      ];
    }
  },
  methods: {},
  created() {},
  watch: {}
};
</script>
