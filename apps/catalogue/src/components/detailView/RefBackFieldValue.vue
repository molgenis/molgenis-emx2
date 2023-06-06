<template>
  <RefbackTable
    :table="metaData.refTable"
    :refBack="metaData.refBack"
    :pkey="metaData.primaryTableKey"
    :refLabel="metaData.refLabel" />
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
    metaData: {
      type: Object,
      required: true,
    },
  },
};
</script>
