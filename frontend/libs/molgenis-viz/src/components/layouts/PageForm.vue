<template>
  <form class="page-form" @submit="(e) => e.preventDefault()">
    <legend class="form-header">
      <span class="form-header-title">{{ title }}</span>
      <span class="form-header-description" v-if="description">{{
        description
      }}</span>
    </legend>
    <div class="form-sections">
      <!-- form content -->
      <slot></slot>
    </div>
  </form>
</template>

<script>
// @displayName PageForm
// The `<PageForm>` component is the parent layout component for creating forms within an page. Ideally, this should be child element of `<PageSection />`.
// @group LAYOUTS
export default {
  name: "PageForm",
  props: {
    // A title for the form (passed down to `<legend>`)
    title: {
      type: String,
      required: true,
    },

    // An optional description of the form to provide additional context
    description: {
      type: String,
      required: false,
    },
  },
};
</script>

<style lang="scss">
.page-form {
  box-sizing: border-box;
  padding: 1.5em 1em;
  border-radius: 6px;
  box-shadow: $box-shadow;

  .form-header {
    display: block;
    margin-bottom: 16px;
    padding-bottom: 4px;
    border-bottom: 1px solid $gray-100;

    .form-header-title {
      display: block;
      font-size: 15pt;
    }

    .form-header-description {
      display: block;
      color: $gray-700;
      line-height: 1.5;
      font-weight: 400;
      font-size: 11pt;
    }
  }
}
</style>
