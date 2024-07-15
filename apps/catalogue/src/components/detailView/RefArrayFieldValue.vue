<template>
  <div>
    <span v-for="(ref, idx) in data" :key="idx">
      <RefFieldValue :data="ref" :metaData="metaData" />&nbsp;
    </span>
  </div>
</template>

<script>
import RefFieldValue from "./RefFieldValue.vue";
export default {
  name: "RefArrayFieldValue",
  components: { RefFieldValue },
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
