<template>
  <div class="cta d-flex flex-column" :style="css.cta.background" ref="cta">
    <section class="m-4" v-html="bodyHtml"></section>
    <section class="m-4 mt-auto">
      <a
        :href="ctaUrl"
        class="btn"
        :class="css.cta.buttonClasses"
        :style="css.cta.buttonStyle"
        role="button"
        aria-pressed="true"
        >{{ ctaText }}</a
      >
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
        cta: {
          backgroundStyle: "background-color: var(--info);",
          buttonClasses: "btn-secondary",
          buttonStyle: "",
        },
      }),
    },
    ctaUrl: {
      type: String,
      required: true,
    },
    ctaText: {
      type: String,
      required: true,
    },
    bodyHtml: {
      type: String,
      required: true,
    },
  },
  mounted() {
    /** retrieving the hexcode from the css property */
    const hexCode = getComputedStyle(this.$refs.cta).getPropertyValue("--info");
    /** adding an opacity */
    this.$refs.cta.style.backgroundColor = `${hexCode}50`;
  },
};
</script>

<style scoped>
.cta {
  width: 30%;
  position: relative;
  border-radius: 1rem;
  min-height: 10rem;
}
</style>
