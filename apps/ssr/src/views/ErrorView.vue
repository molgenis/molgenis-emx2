<template>
  <div>
    <h1>Router error</h1>
    <div>
      Route should be of form /#/table/keyjson/view
      <br />
      <br />For example: /#/Pet/{"name":"spike"}/json ', };}
      <br />
      Got as state = {{ state }}
    </div>
  </div>
</template>

<script>
/* View to render in case of error */
export default {
  props: {
    state: Object,
  },
};
</script>
