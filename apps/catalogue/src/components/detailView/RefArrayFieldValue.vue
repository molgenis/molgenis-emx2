<template>
  <div>
    <span v-for="(ref, idx) in data" :key="idx">
      <RefFieldValue :data="ref" :metadata="metadata" />&nbsp;
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
    metadata: {
      type: Object,
      required: true,
    },
  },
};
</script>
