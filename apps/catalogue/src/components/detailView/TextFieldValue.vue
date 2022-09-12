<template>
  <ReadMore :text="data" :length="1000" />
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  name: "TextFieldValue",
  components: { ReadMore },
  props: {
    data: {
      type: [String, Object],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
  },
};
</script>
