<template>
  <a :href="'mailto:' + data">{{ data }}</a>
</template>

<script>
export default {
  name: "EmailFieldValue",
  props: {
    data: {
      type: [String],
      required: true,
    },
  },
};
</script>
