<template>
  <a v-if="data.id" :href="data.url">
    {{ date?.filename ? data.filename : metaData.name + "." + data.extension }}
    ({{ fileSize }})
  </a>
</template>

<script>
export default {
  name: "FileDisplay",
  props: {
    data: {
      type: [Object],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
  },
  computed: {
    fileSize() {
      const SI_SYMBOL = ["", "k", "M", "G", "T", "P", "E"];

      // what tier? (determines SI symbol)
      const tier = (Math.log10(this.data.size) / 3) | 0;

      // if zero, we don't need a suffix
      if (tier == 0) {
        return this.data.size;
      }

      // get suffix and determine scale
      var suffix = SI_SYMBOL[tier];
      var scale = Math.pow(10, tier * 3);

      // scale the number
      var scaled = this.data.size / scale;

      // format number and add suffix
      return scaled.toFixed(1) + suffix;
    },
  },
};
</script>
