<template>
  <div
    class="hero-image rounded"
    :style="{
      backgroundImage:
        'linear-gradient( ' +
        gradientStart +
        ', ' +
        gradientEnd +
        '  ), url(' +
        imageUrl +
        ')',
    }">
    <div class="hero-text">
      <h1 v-if="title">{{ title }}</h1>

      <p v-if="subTitle">
        {{ subTitle }}
      </p>
    </div>
  </div>
</template>

<script>
export default {
  name: "BannerImage",
  props: {
    imageUrl: {
      type: String,
      required: true,
    },
    title: {
      type: String,
      required: false,
    },
    subTitle: {
      type: String,
      required: false,
    },
  },
  data() {
    return {
      baseColor: "",
    };
  },
  computed: {
    gradientStart() {
      return this.baseColor + "aa";
    },
    gradientEnd() {
      return this.baseColor + "cc";
    },
  },
  mounted: function () {
    if (document && document.documentElement) {
      const docStyle = getComputedStyle(document.documentElement);
      this.baseColor = docStyle.getPropertyValue("--primary");
    }
  },
};
</script>

<style scoped>
/* The hero image */
.hero-image {
  /* Set a specific height */
  height: 200px;

  /* Position and center the image to scale nicely on all screens */
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  position: relative;
}

/* Place text in the middle of the image */
.hero-text {
  text-align: left;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
}
</style>
