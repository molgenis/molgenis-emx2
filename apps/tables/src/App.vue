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
      if (this.schema) return this.schema.label + " / Tables";
      return "Tables";
    },
    schemaId() {
      if (this.schema) return this.schema.id;
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
        "{_schema{id,label,tables{id,label,tableType,schemaId,description,columns{id,label,columnType,key,refTableId,required,description}}}}"
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
