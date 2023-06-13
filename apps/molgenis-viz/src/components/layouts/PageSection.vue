<template>
  <section class="page-section">
    <div :class="classNames">
      <!-- page section content -->
      <slot></slot>
    </div>
  </section>
</template>

<script>
const widthOptions = ["small", "medium", "large", "full"];

// A layout component for creating sections in page.
// @group LAYOUTS
export default {
  props: {
    // Control the width of the default slot
    width: {
      // `'small' / 'medium' / 'large' / 'full'`
      type: String,
      // `medium`
      default: "medium",
      validator: (value) => {
        return widthOptions.includes(value);
      },
    },
    // specify the left and right padding of the default slot
    horizontalPadding: {
      // `0:5`
      type: Number,
      // `1`
      default: 1,
      validator: (value) => {
        return value >= 0 && value <= 5;
      },
    },
    // specify the top and bottom padding of the default slote
    verticalPadding: {
      // `0:5`
      type: Number,
      // `1`
      default: 1,
      validator: (value) => {
        return value >= 0 && value <= 5;
      },
    },
  },
  computed: {
    classNames() {
      const base = "page-section-content";
      const width = `width-${this.width}`;
      const hPadding = `padding-h-${this.horizontalPadding}`;
      const vPadding = `padding-v-${this.verticalPadding}`;
      return [base, width, hPadding, vPadding].join(" ");
    },
  },
};
</script>

<style lang="scss">
.page-section {
  box-sizing: content-box;

  .page-section-content {
    margin: 0 auto;

    h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
      font-weight: 600;
      line-height: 1.3;
      margin: 0;
      text-align: center;
      color: $gray-900;
    }

    p {
      line-height: 1.5;
    }
  }
}
</style>
