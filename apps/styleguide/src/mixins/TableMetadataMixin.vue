<template>
  <ShowMore>
    <pre>error = {{ error }}</pre>
    <pre>user = {{ user }}</pre>
    <pre>schema = {{ schema }}</pre>
  </ShowMore>
</template>

<script>
import { request } from "graphql-request";

export default {
  data: function() {
    return {
      user: null,
      schema: null,
      loading: true,
      error: null
    };
  },
  methods: {
    reloadMetadata() {
      this.loading = true;
      request(
        "graphql",
        "{_user{email}_schema{name,tables{name,columns{name,columnType,key,refTable,cascadeDelete,nullable}}}}"
      )
        .then(data => {
          this.user = data._user;
          this.schema = data._schema;
          this.loading = false;
        })
        .catch(error => {
          this.error = "internal server error" + error;
          this.loading = false;
        });
    }
  },
  created() {
    this.reloadMetadata();
  }
};
</script>

<docs>
    ```
    <TableMetadataMixin/>
    ```
</docs>
