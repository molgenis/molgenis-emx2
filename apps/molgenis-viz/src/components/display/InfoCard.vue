<template>
  <div class="info-card">
    <h3 class="info-card-title">{{ title }}</h3>
    <p class="info-card-text">{{ text }}</p>
    <!-- default content -->
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    // Name of the card
    title: {
      type: String,
      required: true,
    },

    // Text to display
    text: {
      type: String,
      required: true,
    },
  },
};
</script>

<style lang="scss">
.info-card {
  box-sizing: border-box;
  padding: 1.25em;
  background-color: $gray-000;
  border-radius: 6px;

  .info-card-title {
    font-size: 11pt;
    text-transform: uppercase;
    font-weight: 700;
    letter-spacing: 2px;
  }
}
</style>
