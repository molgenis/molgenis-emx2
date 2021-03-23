<template>
  <Molgenis :title="'Up/Download for ' + schema">
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="error">
        {{ error }}
      </MessageError>
      <MessageSuccess v-if="success">
        {{ success }}
      </MessageSuccess>
      <p>
        Import and export data (tables) and metadata (schema, settings) in bulk.
      </p>
      <h4>Upload</h4>
      <div class="mb-2">
        <p>
          Import data by uploading files in excel, zip, json or yaml format.
        </p>
        <form class="form-inline">
          <InputFile v-model="file" />
          <ButtonAction v-if="file != undefined" @click="upload('excel')">
            Import Excel
          </ButtonAction>
          <ButtonAction v-if="file != undefined" @click="upload('zip')">
            Import Zip
          </ButtonAction>
          <ButtonAction v-if="file != undefined" @click="upload('json')">
            Import JSON
          </ButtonAction>
          <ButtonAction v-if="file != undefined" @click="upload('yaml')">
            Import YAML
          </ButtonAction>
        </form>
        <br>
      </div>
      <h4>Download</h4>
      <p>Export data by downloading various file formats:</p>
      <div>
        <p>
          Export schema as <a :href="'../api/csv'">csv</a> /
          <a :href="'../api/json'">json</a> /
          <a :href="'../api/yaml'">yaml</a>
        </p>
        <p>
          Export all data as
          <a :href="'../api/excel'">excel</a> /
          <a :href="'../api/zip'">csv.zip</a> /
          <a :href="'../api/ttl'">ttl</a> /
          <a :href="'../api/jsonld'">jsonld</a>
        </p>
        <div v-if="tables">
          Export specific tables:
          <ul>
            <li v-for="table in tables" :key="table.name">
              {{ table.name }}: <a :href="'../api/csv/' + table.name">csv</a> /
              <a :href="'../api/excel/' + table.name">excel</a>
            </li>
          </ul>
        </div>
        <p>
          Note to programmers: the GET endpoints above also accept http POST
          command for updates, and DELETE commands for deletions.
        </p>
      </div>
    </div>
  </Molgenis>
</template>

<script>
import {request} from 'graphql-request'
import {
  ButtonAction,
  InputFile,
  MessageError,
  MessageSuccess,
  Molgenis,
  Spinner,
} from '@/components/ui/index.js'

export default {
  components: {
    ButtonAction,
    InputFile,
    MessageError,
    MessageSuccess,
    Molgenis,
    Spinner,
  },
  data: function() {
    return {
      error: null,
      file: null,
      loading: false,
      schema: null,
      success: null,
      tables: [],
    }
  },
  created() {
    this.loadSchema()
  },
  methods: {
    loadSchema() {
      this.loading = true
      request('graphql', '{_schema{name,tables{name}}}')
        .then((data) => {
          this.schema = data._schema.name
          this.tables = data._schema.tables
        })
        .catch((error) => {
          this.error = error.response.errors[0].message
        })
        .finally((this.loading = false))
    },
    upload(type) {
      this.error = null
      this.success = null
      this.loading = true
      // upload file contents
      if (['csv', 'json', 'yaml'].includes(type)) {
        let reader = new FileReader()
        reader.readAsText(this.file)
        let url = '/' + this.schema + '/api/' + type
        let _this = this
        reader.onload = function() {
          fetch(url, {
            body: reader.result,
            method: 'POST',
          })
            .then((response) => {
              response.text().then((successText) => {
                _this.success = successText
                _this.error = null
              })
            })
            .catch((error) => {
              error.text().then((errorText) => {
                _this.success = null
                _this.error = 'Failed: ' + errorText
              })
            })
            .finally(() => {
              _this.file = null
              _this.loading = false
            })
        }
      } else {
        let formData = new FormData()
        formData.append('file', this.file)
        let url = '/' + this.schema + '/api/' + type
        fetch(url, {
          body: formData,
          method: 'POST',
        })
          .then((response) => {
            if (response.ok) {
              // todo make proper json
              response.text().then((success) => {
                this.success = success
                this.error = null
              })
            } else {
              response.json().then((error) => {
                this.success = null
                this.error = error.errors[0].message
              })
            }
          })
          .catch((error) => {
            this.error = error
          })
          .finally(() => {
            this.file = null
            this.loading = false
          })
      }
    },
  },
}
</script>

<docs>
Example
```
<Import schema="pet store"/>

```
</docs>
