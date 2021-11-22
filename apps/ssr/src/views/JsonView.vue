<template>
  <div>
    JSON representation of state:<br />
    <pre>{{ JSON.stringify(state, null, 2) }}</pre>
  </div>
</template>

<script>
export default {
  props: {
    state: Object,
  },
};
</script>
