<template>
  <h1>Sub populations: {{ name }}</h1>
</template>

<style scoped></style>

<script>
export default {
  name: "SubCohort",
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
