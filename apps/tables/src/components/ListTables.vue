<template>
  <div>
    <MessageError v-if="!schema"
      >No tables found. Might you need to login?
    </MessageError>
    <DataTable
      v-else
      :columns="['name', 'description']"
      :rows="schema.tables"
      @click="open"
    />
    STATE FOR DEBUG<br />
    molgenis =
    {{ JSON.stringify(molgenis) }}
    <br />
    schema =
    <pre>{{ JSON.stringify(schema, null, "\t") }}</pre>
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
  methods: {
    open(value) {
      open(`#/` + value.name);
    }
  },
  props: {
    molgenis: Object,
    schema: Object
  }
};
</script>

<docs>

</docs>
