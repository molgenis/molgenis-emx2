<template>
  <div class="form-section">
    <!-- form section content -->
    <slot></slot>
  </div>
</template>

<script>
// A layout component for creating sections within a form
// @group LAYOUTS
export default {
  name: "PageFormSection",
};
</script>

<style lang="scss">
.form-section {
  box-sizing: border-box;
  padding-top: 6px;
}
</style>
