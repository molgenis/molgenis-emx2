<template>
  <Molgenis id="__top" v-model="session">
    <MessageWarning v-if="error">{{error}}</MessageWarning>
    <router-view v-else
      :session="session"
      :schema="schema"
      :key="JSON.stringify(session)"
    />
  </Molgenis>
</template>

<script>
import { Molgenis, MessageWarning } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    Molgenis, MessageWarning
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
        "{_schema{name,tables{name,externalSchema,description,columns{name,columnType,key,refTable,required,description}}}}"
      )
        .then((data) => {
          this.schema = data._schema;
        })
        .catch((error) => {
          console.log(JSON.stringify(error))
          if(error.response && error.response.errors[0] && error.response.errors[0].message ) {
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
