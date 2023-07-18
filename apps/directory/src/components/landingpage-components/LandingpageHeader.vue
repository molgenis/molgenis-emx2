<template>
  <div
    class="d-flex flex-column justify-content-center align-items-center"
    :style="css.pageHeader.backgroundStyle"
  >
    <h1 class="mb-5 mt-5">
      <span>{{ headerText }}</span>
    </h1>
    <slot></slot>
    <div class="d-flex mt-auto w-100">
      <a
        class="ml-auto pr-4 pb-3"
        href="#/catalogue"
        :style="css.pageHeader.linkStyle"
        >{{ linkText }}</a
      >
    </div>
  </div>
</template>

<script>
export default {
  props: {
    css: {
      type: Object,
      required: false,
      default: () => ({
        pageHeader: {
          /* https://pixabay.com/illustrations/bacteria-illness-virus-infection-163711/ as default image */
          backgroundStyle:
            'background: url("/plugin/app/molgenis-app-biobank-explorer/img/bacteria.jpg");background-size: cover;height: 30rem;width: 75%;border-radius: 1rem; color: #fff;',
          linkStyle: "color: #fff;",
        },
      }),
    },
    headerText: {
      type: String,
      required: true,
    },
    linkText: {
      type: String,
      required: true,
    },
  },
};
</script>

<style scoped>
::v-deep ~ * {
  width: 70%;
}
</style>
