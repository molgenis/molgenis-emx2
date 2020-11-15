<template>
  <div v-if="schema">
    <h1>Tables: {{ schema.name }}</h1>
    <MessageError v-if="!schema"
      >No tables found. Might you need to login?
    </MessageError>
    Show
    <InputCheckbox
      class="ml-2"
      v-model="tableFilter"
      :defaultValue="tableFilter"
      :options="['external']"
      :clear="false"
    />
    Download:
    <a href="../api/zip" class="ml-2"> zip</a>
    <a href="../api/excel"> Excel</a>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Description</th>
        </tr>
      </thead>
      <tr
        v-for="table in schema.tables.filter(
          table =>
            table.externalSchema == undefined ||
            tableFilter.includes('external')
        )"
        :key="table.name"
      >
        <td>
          <router-link :to="table.name">{{ table.name }}</router-link>
        </td>
        <td>{{ table.description }}</td>
      </tr>
    </table>
  </div>
</template>

<script>
import {
  DataTable,
  InputCheckbox,
  MessageError
} from "@mswertz/emx2-styleguide";

export default {
  name: "App",
  components: {
    DataTable,
    MessageError,
    InputCheckbox
  },
  props: {
    session: Object,
    schema: Object
  },
  data() {
    return {
      tableFilter: []
    };
  }
};
</script>

<docs>

</docs>
