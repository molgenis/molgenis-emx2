<template>
  <RefBackTable
    :refTable="metaData.refTable"
    :refBack="metaData.refBack"
    :pkey="metaData.primaryTableKey"
  />
</template>

<script>
export default {
  name: "RefBackFieldValue",
  props: {
    data: {
      type: [Array],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
  },
};
</script>
