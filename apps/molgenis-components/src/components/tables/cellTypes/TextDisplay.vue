<template>
  <ReadMore :text="data" />
</template>

<script>
// src/components/layout/ReadMore.vue
import ReadMore from "../../layout/ReadMore.vue";
export default {
  name: "TextDisplay",
  components: { ReadMore },
  props: {
    data: {
      type: [String, Object, Array, Number, Boolean],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
  },
};
</script>
