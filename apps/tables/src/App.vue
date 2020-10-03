<template>
  <Molgenis :menuItems="menuItems" id="__top" v-model="session">
    <router-view
      v-if="session && session.email"
      :session="session"
      :schema="schema"
    />
  </Molgenis>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    Molgenis
  },
  data() {
    return {
      session: null,
      schema: null
    };
  },
  computed: {
    title() {
      if (this.schema) return this.schema.name + " / Tables";
      return "Tables";
    },
    schemaName() {
      if (this.schema) return this.schema.name;
      return null;
    },
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
  methods: {
    loadSchema() {
      this.loading = true;
      this.schema = null;
      request(
        "graphql",
        "{_schema{name,tables{name,description,columns{name,columnType,key,refTable,nullable,description}}}}"
      )
        .then(data => {
          this.schema = data._schema;
        })
        .catch(error => {
          if (error.response.error.status === 403) {
            this.error = "Forbidden. Do you need to login?";
          } else this.error = error.response.error;
        })
        .finally((this.loading = false));
    }
  },
  created() {
    this.loadSchema();
  },
  watch: {
    session() {
      this.loadSchema();
    }
  }
};
</script>
