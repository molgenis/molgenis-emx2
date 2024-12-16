<template>
  <div
    class="card text-center img-thumbnail"
    style="width: 10rem; height: 10rem; min-width: 10rem"
  >
    <div class="p-2">
      <img
        :src="imageSrc"
        class="card-img-top"
        alt="altText"
        style="width: 5rem; height: 5rem"
      />
    </div>

    <div class="card-body" style="padding: 5px 5px">
      <router-link v-if="to" :to="to" class="stretched-link"
        ><h6 class="card-title">{{ label }}</h6></router-link
      >
      <h6 class="card-title" v-else>{{ label }}</h6>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouteLocationRaw } from "vue-router";

defineProps<{
  imageSrc: string;
  altText: string;
  label: string;
  to?: RouteLocationRaw;
}>();
</script>

<style scoped>
.card {
  border-radius: 1rem;
  border: #003675 2px solid;
  transition: 0.3s;
  box-shadow: 2px 2px 2px #003675;
}

.card:hover {
  background-color: #f8f9fa;
  cursor: pointer;
  box-shadow: 4px 4px 4px #003675;
  transform: scale(1.1);
}

.card-title {
  color: #003675;
}
</style>
