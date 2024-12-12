<template>
  <div
    class="card text-center img-thumbnail"
    style="width: 10rem; height: 10rem"
  >
    <div class="p-2">
      <img
        :src="imageSrc"
        class="card-img-top"
        alt="altText"
        style="width: 5rem; height: 5rem"
      />
    </div>

    <div class="card-body" style="padding: 10px 10px">
      <h6 class="card-title">{{ label }}</h6>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  imageSrc: string;
  altText: string;
  label: string;
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
  box-shadow: 8px 8px 8px #003675;
  transform: scale(1.2);
}

.card-title {
  color: #003675;
}
</style>
