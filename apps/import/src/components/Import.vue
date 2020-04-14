<template>
  <Spinner v-if="loading" />
  <div v-else>
    {{ loading }}
    <MessageError v-if="error">{{ error }}</MessageError>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    <InputFile v-model="file" :file="file" />
    <ButtonAlt @click="cancel">Cancel</ButtonAlt>
    <ButtonAction @click="upload">Import</ButtonAction>
  </div>
</template>

<script>
import {
  ButtonAction,
  ButtonAlt,
  InputFile,
  MessageError,
  MessageSuccess,
  Spinner
} from '@mswertz/molgenis-emx2-lib-elements'

/** Data import tool */
export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputFile,
    MessageError,
    MessageSuccess,
    Spinner
  },
  props: {
    schema: String
  },
  data: function() {
    return {
      file: null,
      error: null,
      success: null,
      loading: false
    }
  },
  methods: {
    upload() {
      this.error = null
      this.success = null
      this.loading = true
      let form = new FormData()
      form.append('file', this.file)
      let url = '/api/excel/' + this.schema
      fetch(url, {
        method: 'POST',
        body: form
      })
        .then(response => {
          if (response.ok) {
            // todo make proper json
            response.text().then(success => {
              this.success = success
              this.error = null
            })
          } else {
            response.json().then(error => {
              this.success = null
              this.error = error.errors
            })
          }
          this.loading = false
        })
        .catch(error => {
          this.error = error
          this.loading = false
        })
    },
    cancel() {
      this.schemaSelected = null
      this.file = null
    }
  }
}
</script>

<docs>
Example
```
<Import schema="pet store"/>

```
</docs>
