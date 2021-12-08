<template>
<div>Contact</div>
</template>

<script>
export default {
  name: "ContactDetails",
  props: {
    details: {
      type: Object,
      required: false,
      default: () => {},
    },
  },
};
</script>

<style></style>
