<template>
  <Molgenis :title="'Import into ' + schema">
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <InputFile v-model="file" />
      <ButtonAction @click="upload('excel')" :disabled="file == undefined">
        Import Excel
      </ButtonAction>
      <ButtonAction @click="upload('zip')" :disabled="file == undefined">
        Import Zip
      </ButtonAction>
    </div>
  </Molgenis>
</template>

<script>
import {
  ButtonAction,
  ButtonAlt,
  InputFile,
  MessageError,
  MessageSuccess,
  Molgenis,
  Spinner,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

/** Data import tool */
export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputFile,
    MessageError,
    MessageSuccess,
    Spinner,
    Molgenis,
  },
  data: function () {
    return {
      schema: null,
      file: null,
      graphqlError: null,
      success: null,
      loading: false,
    };
  },
  methods: {
    loadSchema() {
      this.loading = true;
      request("graphql", "{_schema{name}}")
        .then((data) => {
          this.schema = data._schema.name;
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    upload(type) {
      this.graphqlError = null;
      this.success = null;
      this.loading = true;
      let formData = new FormData();
      formData.append("file", this.file);
      let url = "/" + this.schema + "/api/" + type;
      fetch(url, {
        method: "POST",
        body: formData,
      })
        .then((response) => {
          if (response.ok) {
            // todo make proper json
            response.text().then((success) => {
              this.success = success;
              this.graphqlError = null;
            });
          } else {
            response.json().then((graphqlError) => {
              this.success = null;
              this.graphqlError = graphqlError.errors[0].message;
            });
          }
        })
        .catch((error) => {
          this.graphqlError = error;
        })
        .finally(() => {
          this.file = null;
          this.loading = false;
        });
    },
  },
  created() {
    this.loadSchema();
  },
};
</script>

<docs>
Example
```
<Import schema="pet store"/>

```
</docs>
