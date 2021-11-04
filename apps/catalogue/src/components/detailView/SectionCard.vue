<template>
  <div class="mt-0">
    <a href="#_top" class="float-right text-white">back to top</a>
    <h3 :class="'pl-2 pr-2 pb-2 mb-0 text-white bg-' + color" :id="meta.name">
      <a>{{ meta.name }}</a>
    </h3>
    <p class="p-2 bg-light mt-0">{{ meta.description }}</p>
  </div>
</template>

<script>
export default {
  name: "SectionCard",
  props: {
    section: {
      type: Object,
      required: true,
    },
    color: { type: String, default: "primary" },
  },
  computed: {
    meta() {
      return this.section.meta;
    },
  },
};
</script>
