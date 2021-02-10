<template>
  <ShowMore>
    <pre>error = {{ error }}</pre>
    <pre>session = {    { session }}</pre>
    <pre>schema = {{ schema }}</pre>
  </ShowMore>
</template>

<script>
import { request } from "graphql-request";

export default {
  props: {
    graphqlURL: {
      default: "graphql",
      type: String,
    },
  },
  data: function () {
    return {
      session: null,
      schema: null,
      loading: true,
      error: null,
    };
  },
  methods: {
    reloadMetadata() {
      this.loading = true;
      request(
        this.graphqlURL,
        "{_session{email,roles}_schema{name,tables{name,columns{name,columnType,key,refTable,refLink,refJsTemplate,required,jsonldType}}}}"
      )
        .then((data) => {
          this.session = data._session;
          this.schema = data._schema;
          this.loading = false;
        })
        .catch((error) => {
          this.error = "internal server error" + error;
          this.loading = false;
        });
    },
  },
  created() {
    this.reloadMetadata();
  },
};
</script>

<docs>
Normally you would not instantiate a mixin component, so this is only for quick testing
```
<!-- in normal use you don't need graphqlURL prop -->
<TableMetadataMixin graphqlURL="/pet store/graphql"/>
```
</docs>
