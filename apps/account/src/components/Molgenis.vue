<template>
  <Spinner v-if="loading" />
  <MessageError v-else-if="error">{{ error }}</MessageError>
  <div v-else>
    <div class="container" style="margin-top: 60px;">
      <div v-if="view === 'home'">
        <div class="jumbotron">
          <h1 class="display-4">Welcome on the MOLGENIS EMX 2.0 preview.</h1>
          <p class="lead">Tables, schemas, graphql. That's it.</p>
          <p>
            Choose a schema to continue
            <InputSelect v-model="schema" :items="schemaList" />
          </p>
        </div>
      </div>
      <Explorer v-if="view === 'explorer'" :schema="schema" />
      <!--Schema v-if="view === 'schema'" :schema="schema" /-->
      <Import v-if="view === 'import'" :schema="schema" />
    </div>
  </div>
</template>

<script>
import { request } from 'graphql-request'

import { InputSelect, MessageError } from '@mswertz/molgenis-emx2-lib-elements'

import Explorer from '../organisms/Explorer.vue'
// import Schema from '../organisms/Schema.vue'
// import Account from '../organisms/Account.vue'
import Import from '../organisms/Import.vue'

export default {
  components: {
    Explorer,
    // Schema,
    // Account,
    Import,
    InputSelect,
    MessageError
  },
  data: function() {
    return {
      view: 'home',
      schema: null,
      schemaList: [],
      error: null,
      loading: false
    }
  },
  computed: {
    account() {
      return this.$store.state.account.email
    }
  },
  watch: {
    account() {
      this.getSchemaList()
    }
  },
  created() {
    this.getSchemaList()
  },
  methods: {
    getSchemaList() {
      this.loading = true
      request('/api/graphql', '{Schemas{name}}')
        .then(data => {
          this.schemaList = data.Schemas.map(schema => schema.name)
        })
        .catch(error => (this.error = 'internal server error' + error))
      this.loading = false
    }
  }
}
</script>

<docs>
Example
```
<Molgenis/>
```
</docs>
