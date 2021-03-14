<template>
  <div v-if="schema">
    <h1>Tables in '{{ schema.name }}'</h1>
    <MessageError v-if="!schema">
      No tables found. Might you need to login?
    </MessageError>
    <label v-else>{{ count }} tables found</label>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">
            Table
            <div class="form-check form-check-inline">
              <InputCheckbox
                class="ml-2"
                v-model="tableFilter"
                :defaultValue="tableFilter"
                :options="['external']"
                :clear="false"
              />
            </div>
          </th>
          <th scope="col" v-if="tableFilter.includes('external')">
            externalSchema
          </th>
          <th scope="col">Description</th>
        </tr>
      </thead>
      <tr
        v-for="table in schema.tables.filter(
          (table) =>
            table.externalSchema == undefined ||
            tableFilter.includes('external')
        )"
        :key="table.name"
      >
        <td>
          <router-link :to="table.name">{{ table.name }}</router-link>
        </td>
        <td v-if="tableFilter.includes('external')">
          {{ table.externalSchema }}
        </td>
        <td>{{ table.description }}</td>
      </tr>
    </table>
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
