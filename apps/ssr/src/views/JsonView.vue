<template>
  <div>
    <h1>JsonView:</h1>
    state.row =
    <pre>{{ JSON.stringify(state.row, null, 2) }}</pre>
    state.schema =
    <pre>{{ JSON.stringify(state.schema, null, 2) }}</pre>
  </div>
</template>

<script>
/* View for testing purposes, and also to demonstrate routing works */
export default {
  props: {
    state: Object,
  },
};
</script>
