<template>
  <RefbackTable
    :tableId="metadata.refTableId"
    :refBackId="metadata.refBackId"
    :pkey="metadata.primaryTableKey"
    :refLabel="metadata.refLabel"
  />
</template>

<script>
import RefbackTable from "../RefbackTable.vue";
export default {
  name: "RefBackFieldValue",
  components: { RefbackTable },
  props: {
    data: {
      type: [Object, Array],
      required: true,
    },
    metadata: {
      type: Object,
      required: true,
    },
  },
};
</script>
