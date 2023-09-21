<template>
  <div>{{ data }}</div>
</template>

<script>
export default {
  name: "StringDisplay",
  props: {
    data: {
      type: [String, Object, Number, Boolean],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
  },
};
</script>
