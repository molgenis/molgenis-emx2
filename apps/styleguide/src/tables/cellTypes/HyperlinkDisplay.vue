<template>
  <a target="_blank" :href="data">{{ data }}</a>
</template>

<script>
export default {
  name: "HyperlinkDisplay",
  props: {
    data: {
      type: String,
      required: true,
    },
  },
};
</script>