<template>
  <div
    :style="css.biobankSpotlight.backgroundStyle"
    class="d-flex flex-column pl-4"
  >
    <h1 class="mt-4 header-text">
      <span>{{ headerText }}</span>
      <p>{{ biobankName }}</p>
    </h1>
    <div v-if="bodyHtml" v-html="bodyHtml"></div>
    <section class="mt-auto mb-4">
      <router-link
        :to="'/biobank/' + biobankId"
        :title="`Go to ${biobankName}`"
        class="btn"
        :class="css.biobankSpotlight.buttonClasses"
        :style="buttonStyle"
      >
        <span>{{ buttonText }}</span>
      </router-link>
    </section>
  </div>
</template>

<script>
export default {
  props: {
    css: {
      type: Object,
      required: false,
      default: () => ({
        biobankSpotlight: {
          /* https://www.pexels.com/photo/close-up-of-microscope-256262/ as default image */
          backgroundStyle:
            'background: url("/plugin/app/molgenis-app-biobank-explorer/img/microscope.jpg");background-size: cover;height: 30rem;width: 50%;border-radius: 1rem; color: #fff;',
          buttonClasses: "btn-primary",
          buttonStyle: "",
        },
      }),
    },
    headerText: {
      type: String,
      required: true,
    },
    biobankName: {
      type: String,
      required: true,
    },
    biobankId: {
      type: String,
      required: true,
    },
    bodyHtml: {
      type: String,
      required: false,
    },
    buttonText: {
      type: String,
      required: true,
    },
  },
};
</script>

<style scoped>
.header-text {
  font-size: 1.6rem;
}

.header-text p {
  font-size: 2.5rem;
  font-weight: bold;
}
</style>
