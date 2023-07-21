<template>
  <div></div>
</template>

<script>
import { setOptions, bootstrap } from "vue-gtag";
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
        pageTrackerScreenviewEnabled: true,
      });

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
