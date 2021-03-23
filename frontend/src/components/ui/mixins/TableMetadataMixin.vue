<template>
  <ShowMore>
    <pre>graphqlError = {{ graphqlError }}</pre>
    <pre>session = {    { session }}</pre>
    <pre>schema = {{ schema }}</pre>
  </ShowMore>
</template>

<script>
import {request} from 'graphql-request'

export default {
  props: {
    graphqlURL: {
      default: 'graphql',
      type: String,
    },
  },
  data: function() {
    return {
      graphqlError: null,
      loading: true,
      schema: null,
      session: null,
    }
  },
  computed: {
    canEdit() {
      return (
        this.session &&
        (this.session.email == 'admin' ||
          (this.session.roles &&
            (this.session.roles.includes('Editor') ||
              this.session.roles.includes('Manager'))))
      )
    },
  },
  created() {
    this.reloadMetadata()
  },
  methods: {
    reloadMetadata() {
      this.loading = true
      this.graphqlError = null
      request(
        this.graphqlURL,
        '{_session{email,roles}_schema{name,tables{name,description,semantics,columns{name,columnType,key,refTable,refLink,refJsTemplate,required,semantics}}}}',
      )
        .then((data) => {
          this.session = data._session
          this.schema = data._schema
          this.loading = false
        })
        .catch((error) => {
          this.graphqlError = 'internal server graphqlError' + error
          this.loading = false
        })
    },
  },
}
</script>
