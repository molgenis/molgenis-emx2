<template>
  <ReadMore :text="data" />
</template>

<script>
import ReadMore from "../../layout/ReadMore";
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
