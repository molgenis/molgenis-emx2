<template>
  <div v-if="schema" class="container">
    <h1>Tables in '{{ schema.name }}'</h1>
    <MessageWarning v-if="!schema">
      No tables found. Might you need to sign in?
    </MessageWarning>
    <MessageWarning v-if="!schema.tables">
      No tables found. You might want to go to design
      <a href="../schema/">design</a> or
      <a href="../updownload/">upload</a> your schema to create them.
    </MessageWarning>
    <div v-else>
      Download all tables:
      <a href="../api/zip">zip</a> | <a href="../api/excel">excel</a> |
      <a href="../api/jsonld">jsonld</a> | <a href="../api/ttl">ttl</a><br />
      <table class="table bg-white table-hover" v-if="schema.tables">
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
          @click="router.push({ path: table.name })"
        >
          <td>
            <router-link :to="table.name"> {{ table.name }}</router-link>
          </td>
          <td>{{ table.description }}</td>
        </tr>
      </table>
    </div>
    <ShowMore title="debug">
      session: {{ session }} <br /><br />
      schema: {{ schema }}
    </ShowMore>
  </div>
</template>

<script>
import {
  ButtonDropdown,
  DataTable,
  InputCheckbox,
  MessageWarning,
  ShowMore,
} from "@mswertz/emx2-styleguide";

export default {
  name: "App",
  components: {
    DataTable,
    MessageWarning,
    InputCheckbox,
    ButtonDropdown,
    ShowMore,
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
