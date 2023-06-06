<template>
  <div>
    <h1>Welcome to genome data portal</h1>
    <RouterLink to="/beacon">Go to beacon</RouterLink>
  </div>
</template>

<script>
import { RouterLink } from "vue-router";

export default {
  components: {
    RouterLink,
  },
  data() {
    return {};
  },
};
</script>
