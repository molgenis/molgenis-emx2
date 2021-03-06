<template>
  <Molgenis id="__top" v-model="session">
    <router-view
      v-if="session"
      :session="session"
      :schema="schema"
      :key="JSON.stringify(session)"
    />
    <ShowMore title="debug">
      session: {{ session }} <br /><br />
      schema: {{ schema }}
    </ShowMore>
  </Molgenis>
</template>

<script>
import { Molgenis, ShowMore } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    Molgenis,
    ShowMore,
  },
  data() {
    return {
      session: null,
      schema: null,
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
      request(
        "graphql",
        "{_schema{name,tables{name,externalSchema,description,columns{name,columnType,key,refTable,required,description}}}}"
      )
        .then((data) => {
          this.schema = data._schema;
        })
        .catch((error) => {
          if (error.response.error.status === 403) {
            this.graphqlError = "Forbidden. Do you need to login?";
          } else this.graphqlError = error.response.error;
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
