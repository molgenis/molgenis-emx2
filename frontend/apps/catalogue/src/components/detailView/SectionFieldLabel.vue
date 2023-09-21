<template>
  <div class="text-right labelcontainer">
    <div :class="'tooltip bs-tooltip-top show bg-' + color" role="tooltip">
      <div v-if="tooltip" :class="'tooltip-inner bg-' + color">
        {{ tooltip }}
      </div>
    </div>
    <label class="mb-0 font-weight-bold">{{ asSentence }}: </label>
  </div>
</template>

<script>
export default {
  name: "SectionFieldLabel",
  props: {
    label: {
      type: String,
      required: true,
    },
    tooltip: {
      type: String,
      required: false,
    },
    color: {
      type: String,
      default: () => "primary",
    },
  },
  computed: {
    asSentence() {
      return (
        this.label
          // insert a space before all caps
          .replace(/([A-Z])/g, " $1")
          // uppercase the first character
          .replace(/^./, (str) => str.toUpperCase())
      );
    },
  },
};
</script>

<style>
.labelcontainer .tooltip {
  visibility: hidden;
}

.labelcontainer:hover .tooltip {
  visibility: visible;
}
</style>
