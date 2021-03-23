<template>
  <div v-if="schema">
    <h1>Tables in '{{ schema.name }}'</h1>
    <MessageError v-if="!schema">
      No tables found. Might you need to login?
    </MessageError>
    Download all tables:
    <a href="../api/zip">zip</a> | <a href="../api/excel">excel</a> |
    <a href="../api/jsonld">jsonld</a> | <a href="../api/ttl">ttl</a><br>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">
            Table
            <div class="form-check form-check-inline">
              <InputCheckbox
                v-model="tableFilter"
                class="ml-2"
                :clear="false"
                :default-value="tableFilter"
                :options="['external']"
              />
            </div>
          </th>
          <th v-if="tableFilter.includes('external')" scope="col">
            externalSchema
          </th>
          <th scope="col">
            Description
          </th>
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
          <router-link :to="table.name">
            {{ table.name }}
          </router-link>
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
import {InputCheckbox, MessageError} from '@/components/ui/index.js'

export default {
  components: {
    InputCheckbox,
    MessageError,
  },
  props: {
    schema: Object,
    session: Object,
  },
  data() {
    return {
      tableFilter: [],
    }
  },
  computed: {
    count() {
      if (!this.schema || !this.schema.tables) {
        return 0
      }
      return this.schema.tables.length
    },
  },
}
</script>
