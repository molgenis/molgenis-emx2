<template>
  <div>
    <h1>Pet = {{ state.row.name }}</h1>
    {{ state.row }}
  </div>
</template>
<script>
/* Demonstration of custom views using nicer routes */
export default {
  props: {
    state: Object,
  },
};
</script>
