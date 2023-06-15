<template>
  <div>
    <h1>Welcome to genome data portal</h1>
    <div>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
      veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
      commodo consequat. Duis aute irure dolor in reprehenderit in voluptate
      velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
      cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
      est laborum.
    </div>
    <RouterLink to="/datasets" class="btn btn-primary mr-2"
      >Navigate datasets</RouterLink
    >
    <RouterLink to="/beacon" class="btn btn-primary">Go to beacon</RouterLink>
  </div>
</template>

<script setup>
import { RouterLink } from "vue-router";
</script>
