<template>
  <a :href="data">{{ data }}</a>
</template>

<script>
export default {
  name: "HyperlinkFieldValue",
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
