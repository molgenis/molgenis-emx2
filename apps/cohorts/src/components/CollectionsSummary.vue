<template>
  <div>
    <h4>{{ collection.title }}</h4>
    <p>{{ collection.description }}</p>
  </div>
</template>

<script>
export default {
  props: {
    collection: Object,
  },
};
</script>
