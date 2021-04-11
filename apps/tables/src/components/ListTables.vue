<template>
  <div v-if="schema">
    <h1>Tables in '{{ schema.name }}'</h1>
    <MessageError v-if="!schema">
      No tables found. Might you need to login?
    </MessageError>
    <MessageError v-if="!schema.tables">
      No tables found. Might you need to create them?
    </MessageError>
    <div v-else>
      Download all tables:
      <a href="../api/zip">zip</a> | <a href="../api/excel">excel</a> |
      <a href="../api/jsonld">jsonld</a> | <a href="../api/ttl">ttl</a><br />
      <table class="table bg-white" v-if="schema.tables">
        <thead>
          <tr>
            <th scope="col">Table</th>
            <th scope="col">Description</th>
          </tr>
        </thead>
        <tr
          v-for="table in schema.tables.filter(
            (table) => table.externalSchema == undefined
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
  </div>
</template>

<script>
import {
  ButtonDropdown,
  DataTable,
  InputCheckbox,
  MessageError,
} from "@mswertz/emx2-styleguide";

export default {
  name: "App",
  components: {
    DataTable,
    MessageError,
    InputCheckbox,
    ButtonDropdown,
  },
  props: {
    session: Object,
    schema: Object,
  },
  data() {
    return {
      tableFilter: [],
    };
  },
  computed: {
    count() {
      if (!this.schema || !this.schema.tables) {
        return 0;
      }
      return this.schema.tables.length;
    },
  },
};
</script>

<docs>

</docs>
