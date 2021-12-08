<template>
  <div>
    <div class="row">
      <div class="col-9">
        <h1>{{ title }}</h1>
      </div>

      <div class="col-3 d-flex flex-row-reverse">
        <img
          v-if="logoUrl"
          class="justify-content-center align-self-center"
          :src="logoUrl"
          alt="cohort logo"
        />
      </div>
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
img {
  /* same as heading */
  height: 2.5rem;
}
</style>
