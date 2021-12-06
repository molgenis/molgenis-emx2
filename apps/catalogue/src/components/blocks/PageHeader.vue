<template>
  <div class="row">
    <div class="col-9">
      <h1>{{ title }}</h1>
    </div>

    <div class="col-3">
      <img v-if="logoUrl" :src="logoUrl" class="" alt="cohort logo" />
    </div>
  </div>
</template>

<script>
export default {
  name: "PageHeader",
  props: {
    title: {
      type: String,
      required: true,
    },
    logoUrl: {
      type: String,
      required: false,
    },
  },
};
</script>

<style scoped>
.row .col-9 {
  padding-left: 0;
}
img {
  /* same as heading */
  height: 2.5rem;
}
</style>
