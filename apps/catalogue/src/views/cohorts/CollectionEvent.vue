<template>
  <h1>Collection Event: {{ name }}</h1>
</template>

<style scoped></style>

<script>
export default {
  name: "CohortView",
  props: {
    cohort: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
  },
};
</script>
