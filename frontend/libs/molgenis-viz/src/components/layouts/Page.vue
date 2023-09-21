<template>
  <main class="app-page">
    <!-- Main app content -->
    <slot></slot>
  </main>
</template>

<script>
// The `<Page/>` component is the primary layout component for creating pages within a vue application. Pages should then be built using other page layout components (`<PageHeader>`, `<PageSection>`, `<PageFooter>`, etc.). For example, if we were to outline a typical structure of a page, it would look like this.
//
// If you have more than one page, it is recommended to create a custom footer, and import it at the `App.vue` level. See the PageFooter guide for more details.
//
// @group LAYOUTS
//
export default {
  name: "AppPage",
};
</script>

<style lang="scss">
.app-page {
  font-size: 15pt;
  color: $gray-700;
  min-height: 100vh;
}
</style>
