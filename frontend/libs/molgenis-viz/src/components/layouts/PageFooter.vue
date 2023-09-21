<template>
  <footer class="page-footer">
    <!-- main footer content -->
    <slot></slot>
    <PageFooterMolgenisCitation />
  </footer>
</template>

<script>
import PageFooterMolgenisCitation from "./PageFooterMolgenisCitation.vue";

// A layout component to define the page footer. By default, the MOLGENIS
// citation component is rendered in the app, which displays the MOLGENIS
// version and release date.
//
// For applications with more than one page, it is recommended to create
// a new footer and import it at the `App.vue` level.
//
// @group LAYOUTS
export default {
  components: {
    PageFooterMolgenisCitation,
  },
};
</script>
