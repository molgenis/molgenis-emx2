<template>
  <button class="btn btn-outline-primary mr-2 mb-2" @click="$emit('click')">
    {{ label }} &nbsp;<i :class="'fa fa-times'" />
  </button>
</template>

<script>
export default {
  props: {
    label: String,
  },
}
</script>

<style scoped>
.btn i {
  visibility: hidden;
}

.btn:hover i {
  visibility: visible;
}
</style>
