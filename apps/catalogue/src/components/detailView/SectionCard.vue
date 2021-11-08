<template>
  <div class="mt-0">
    <a href="#_top" class="float-right text-white">back to top</a>
    <h3 :class="'pl-2 pr-2 pb-2 mb-0 text-white bg-' + color" :id="meta.name">
      <a>{{ meta.name }}</a>
    </h3>
    <p class="p-2 bg-light mt-0">{{ meta.description }}</p>
    <div class="p-0" v-for="(field, index) in fields" :key="index">
      <section-item :field="field" :color="color"></section-item>
    </div>
  </div>
</template>

<script>
import SectionItem from "./SectionItem.vue";

export default {
  name: "SectionCard",
  components: { SectionItem },
  props: {
    section: {
      type: Object,
      required: true,
    },
    hideNA: {
      type: Boolean,
      default: () => false,
    },
    color: { type: String, default: () => "primary" },
  },
  computed: {
    meta() {
      return this.section.meta;
    },
    fields() {
      return this.section.fields;
    },
  },
};
</script>
