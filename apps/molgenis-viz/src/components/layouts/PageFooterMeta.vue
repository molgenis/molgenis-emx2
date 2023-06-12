<template>
  <div class="page-footer-meta-links">
    <!-- main content -->
    <slot></slot>
  </div>
</template>

<script>
// This is a layout component for use in the `<PageFooter/>` component. The purpose is to display links to pages within and outside the application. It is recommended to use the `<UnorderedList />` component to define the links.
// @group LAYOUTS
export default {
  name: 'PageFooterMeta'
}
</script>

<style lang="scss">
.page-footer-meta-links {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: $gray-100;
  padding: 0.8em;
  font-size: 12pt;
}
</style>
