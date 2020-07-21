<template>
  <div>nothing</div>
</template>

<script>
import { request } from "graphql-request";

export default {
  props: {
    table: String
  },
  data: function() {
    return {
      metadata: {},
      loading: true,
      error: null
    };
  },
  methods: {
    reloadMetadata() {
      this.loading = true;
      request(
        "graphql",
        "{_schema{tables{name,columns{name,columnType,key,refTable,refColumns,cascadeDelete,nullable}}}}"
      )
        .then(data => {
          data._schema.tables.forEach(element => {
            if (element.name === this.table) {
              this.metadata = element;
            }
          });
          this.loading = false;
        })
        .catch(error => {
          this.error = "internal server error" + error;
          this.loading = false;
        });
    }
  },
  watch: {
    table: "reloadMetadata"
  },
  created() {
    this.reloadMetadata();
  }
};
</script>
