<template>
  <div>
    Route should be of form /#/table/keyjson/view
    <br />
    <br />For example: /#/Pet/{"name":"spike"}/json ', };}
    <br />
    Got as state = {{ state }}
  </div>
</template>

<script>
export default {
  props: {
    state: Object,
  },
};
</script>
