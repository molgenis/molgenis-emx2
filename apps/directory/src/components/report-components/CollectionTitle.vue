<template>
  <h4 class="py-2">
    <router-link :to="'/collection/' + id">
      {{ title }}
    </router-link>
  </h4>
</template>

<script>
export default {
  name: 'CollectionTitle',
  props: {
    id: {
      type: String,
      required: true
    },
    title: {
      type: String,
      required: true
    }
  }
}
</script>
