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
      <InputSearch
        v-if="count > 10"
        placholder="search by name"
        v-model="search"
      />
      <table class="table bg-white table-hover" v-if="tablesFiltered">
        <thead>
          <tr>
            <th scope="col">Table</th>
            <th scope="col">Description</th>
          </tr>
        </thead>
        <tr
          v-for="table in tablesFiltered"
          :key="table.name"
        >
          <td>
            <router-link :to="table.name"> {{ table.name }}</router-link>
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
  MessageWarning,
  InputSearch,
} from "@mswertz/emx2-styleguide";

export default {
  name: "App",
  components: {
    DataTable,
    MessageWarning,
    InputCheckbox,
    ButtonDropdown,
    InputSearch,
  },
  props: {
    session: Object,
    schema: Object,
  },
  data() {
    return {
      tableFilter: [],
      search: null,
    };
  },
  computed: {
    count() {
      if (!this.schema || !this.schema.tables) {
        return 0;
      }
      return this.schema.tables.length;
    },
    tablesFiltered() {
      if (!this.schema || !this.schema.tables) {
        return [];
      }
      if (this.search && this.search.trim().length > 0) {
        let terms = this.search.toLowerCase().split(" ");
        return this.schema.tables
          .filter((table) => !table.externalSchema)
          .filter((t) =>
            terms.every(
              (v) =>
                t.name.toLowerCase().includes(v) ||
                (t.description && t.description.toLowerCase().includes(v))
            )
          );
      } else {
        return this.schema.tables.filter((table) => !table.externalSchema);
      }
    },
  },
};
</script>

<docs>

</docs>
