<template>
  <Molgenis :menuItems="menuItems" id="__top" v-model="molgenis">
    <router-view :molgenis="molgenis" :schema="schema" />
  </Molgenis>
</template>

<script>
    import {Molgenis} from "@mswertz/emx2-styleguide";
    import {request} from "graphql-request";

    export default {
  components: {
    Molgenis
  },
  data() {
    return {
      molgenis: null,
      schema: null
    };
  },
  computed: {
    title() {
      if (this.schema) return this.schema.name + " / Tables";
      return "Tables";
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
          href: "/api/playground.html?schema=/api/graphql/" + this.schema
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
        "{_schema{name,tables{name,pkey,description,columns{name,columnType,pkey,refTable,refColumn,nullable,description}}}}"
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
    molgenis() {
      this.loadSchema();
    }
  }
};
</script>
