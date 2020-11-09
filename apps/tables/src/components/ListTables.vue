<template>
  <div v-if="schema">
    <h1>{{ schema.name }}</h1>
    <p>Tables:</p>
    <MessageError v-if="!schema"
      >No tables found. Might you need to login?
    </MessageError>
    Download:
    <a href="../api/zip"> zip</a>
    <a href="../api/excel"> Excel</a>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Description</th>
        </tr>
      </thead>
      <tr v-for="table in schema.tables" :key="table.name">
        <td>
          <router-link :to="table.name">{{ table.name }}</router-link>
        </td>
        <td>{{ table.description }}</td>
      </tr>
    </table>
  </div>
</template>

<script>
import { DataTable, MessageError } from "@mswertz/emx2-styleguide";

export default {
  name: "App",
  components: {
    DataTable,
    MessageError
  },
  props: {
    session: Object,
    schema: Object
  }
};
</script>

<docs>

</docs>
