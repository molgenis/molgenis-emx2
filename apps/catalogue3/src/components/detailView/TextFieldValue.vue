<template>
  <ReadMore :text="data" :length="1000" />
</template>

<script>
import { ReadMore } from "molgenis-components";

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
