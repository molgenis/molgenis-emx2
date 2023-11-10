<template>
  <div></div>
</template>

<script>
import { setOptions, bootstrap } from "vue-gtag";
import { pageview } from "vue-gtag";

export default {
  name: "GTag",
  props: {
    tagId: {
      type: String,
      required: true,
    },
  },
  methods: {
    enablePlugin(tagId) {
      setOptions({
        config: { id: tagId },
      });

      bootstrap().then(() =>
        console.log("start analytics for tagId: " + tagId)
      );

      if (this.$router) {
        this.$router.afterEach((to) => {
          pageview(to.fullPath);
        });
      }

      bootstrap().then((gtag) => {
        console.log("start analytics for tagId: " + tagId);
      });

    },
  },
  async mounted() {
    this.enablePlugin(this.tagId);
  },
};
</script>
