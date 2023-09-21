<template>
  <a :href="`mailto:${data}`">{{ data }}</a>
</template>

<script>
export default {
  name: "EmailDisplay",
  props: {
    data: {
      type: String,
      required: true,
    },
  },
};
</script>
