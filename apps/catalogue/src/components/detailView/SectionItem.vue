<template>
  <div class="m-2 showcontainer row">
    <sectionItemLabel
      class="col-4"
      :label="field.meta.name"
      :tooltip="field.description"
      :color="color"
    ></sectionItemLabel>

    <div class="col-8">
      {{ field.value }}
    </div>
  </div>
</template>

<script>
import SectionItemLabel from "./SectionItemLabel.vue";

export default {
  name: "SectionItem",
  components: { SectionItemLabel },
  props: {
    field: {
      type: Object,
      required: true,
    },
    color: {
      type: String,
      default: () => "primary",
    },
  },
};
</script>
