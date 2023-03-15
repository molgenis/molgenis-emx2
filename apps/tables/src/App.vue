<template>
  <Molgenis id="__top" v-model="session">
    <MessageWarning v-if="error">{{ error }}</MessageWarning>
    <router-view
      v-else
      :session="session"
      :schema="schema"
      :key="JSON.stringify(session)"
    />
  </Molgenis>
</template>

<script>
import { Molgenis, MessageWarning } from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    Molgenis,
    MessageWarning,
  },
  data() {
    return {
      session: null,
      schema: null,
      error: null,
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
  },
  methods: {
    loadSchema() {
      this.loading = true;
      this.schema = null;
      this.error = null;
      request(
        "graphql",
        "{_schema{name,tables{id,name,tableType,externalSchema,labels{locale,value},descriptions{locale,value},columns{name,labels{locale,value}columnType,key,refTable,required,descriptions{locale,value}}}}}"
      )
        .then((data) => {
          this.schema = data._schema;
        })
        .catch((error) => {
          console.log(JSON.stringify(error));
          if (
            error.response &&
            error.response.errors[0] &&
            error.response.errors[0].message
          ) {
            this.error = error.response.errors[0].message;
          } else {
            this.error = error;
          }
        })
        .finally((this.loading = false));
    },
  },
  created() {
    this.loadSchema();
  },
  watch: {
    session() {
      this.loadSchema();
    },
  },
};
</script>
