<template>
  <div>
    <!--router-view /-->
    blabla
    {{ state }}
  </div>
</template>

<script>
//expect state
export default {
  data() {
    return {
      graphql: null,
      state, //global, injected by the ssr render
    };
  },
};
</script>
