<template>
  <div>
    <h3>For testing purposes</h3>
    <p>TableMetadataMixin</p>
    <pre>session: {{ session }}</pre>
    <pre>schema: <ShowMore title="schema">{{ schema }}</ShowMore></pre>
    <pre>error: {{ graphqlError }}</pre>
  </div>
</template>

<script>
import {request} from 'graphql-request';
import ShowMore from '../layout/ShowMore';

export default {
  components: {
    ShowMore
  },
  props: {
    graphqlURL: {
      default: 'graphql',
      type: String
    }
  },
  data: function () {
    return {
      session: null,
      schema: null,
      loading: true,
      graphqlError: null
    };
  },
  methods: {
    reloadMetadata() {
      this.loading = true;
      this.graphqlError = null;
      request(
        this.graphqlURL,
        `{
          _session { email,roles } _schema {
            name, tables {
              name, tableType, id, description, externalSchema, semantics, columns {
                name, id, columnType, key, refTable, refLink, refLabel, refBack, required, 
                semantics, description, position, validation, visible
              } settings { key, value }
            }
          }
        }`
      )
        .then((data) => {
          this.session = data._session;
          this.schema = data._schema;
          this.loading = false;
        })
        .catch((error) => {
          if (Array.isArray(error.response.errors)) {
            this.graphqlError = error.response.errors[0].message;
          } else {
            this.graphqlError = error;
          }
          this.loading = false;
        });
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
      );
    },
    canManage() {
      return (
        this.session &&
        (this.session.email == 'admin' ||
          this.session.roles.includes('Manager'))
      );
    }
  },
  created() {
    this.reloadMetadata();
  }
};
</script>

<docs>
Normally you would not instantiate a mixin component, so this is only for quick testing
```
<!-- in normal use you don't need graphqlURL prop -->
<TableMetadataMixin table="Pet" graphqlURL="/pet store/graphql"/>
```
</docs>
