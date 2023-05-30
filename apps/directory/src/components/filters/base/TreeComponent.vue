<template>
  <ul></ul>
</template>

<script>
export default {
  name: "TreeComponent",
  props: {
    options: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      tree: [],
    };
  },
  // weed out parents, and take that as a basis
  // then connect the children and their children.
  // render the stuff
};
</script>
