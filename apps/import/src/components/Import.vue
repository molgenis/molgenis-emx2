<template>
  <Molgenis :title="'Import into ' + schema" :menuItems="menuItems">
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <InputFile v-model="file" />
      <ButtonAction @click="upload" :disabled="file == undefined">
        Import
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
  Spinner
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
    Molgenis
  },
  data: function() {
    return {
      schema: null,
      file: null,
      error: null,
      success: null,
      loading: false
    };
  },
  methods: {
    loadSchema() {
      this.loading = true;
      request("graphql", "{_schema{name}}")
        .then(data => {
          this.schema = data._schema.name;
        })
        .catch(error => {
          this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    upload() {
      this.error = null;
      this.success = null;
      this.loading = true;
      let formData = new FormData();
      formData.append("file", this.file);
      let url = "/api/excel/" + this.schema;
      fetch(url, {
        method: "POST",
        body: formData
      })
        .then(response => {
          if (response.ok) {
            // todo make proper json
            response.text().then(success => {
              this.success = success;
              this.error = null;
            });
          } else {
            response.json().then(error => {
              this.success = null;
              this.error = error.errors[0].message;
            });
          }
        })
        .catch(error => {
          this.error = error;
        })
        .finally(() => {
          this.file = null;
          this.loading = false;
        });
    }
  },
  created() {
    this.loadSchema();
  }
};
</script>

<docs>
Example
```
<Import schema="pet store"/>

```
</docs>
