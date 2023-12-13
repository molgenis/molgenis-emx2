<template>
  <div class="mt-0">
    <a
      v-if="showCardHeader"
      href="#"
      v-scroll-to="{
        el: '#_top',
      }"
      class="float-right text-white"
    >
      back to top
    </a>
    <h3
      v-if="showCardHeader"
      :class="'pl-2 pr-2 pb-2 mb-0 text-white bg-' + color"
      :id="meta.id"
    >
      <a>{{ meta.label }}</a>
    </h3>
    <p v-if="showCardHeader" class="p-2 bg-light mt-0">
      {{ meta.description }}
    </p>
    <div class="p-0" v-for="(field, index) in fields" :key="index">
      <section-field :field="field" :color="color"></section-field>
    </div>
  </div>
</template>

<script>
import SectionField from "./SectionField.vue";

export default {
  name: "SectionCard",
  components: { SectionField },
  props: {
    section: {
      type: Object,
      required: true,
    },
    hideNA: {
      type: Boolean,
      default: () => false,
    },
    showCardHeader: {
      type: Boolean,
      default: () => true,
    },
    color: { type: String, default: () => "primary" },
  },
  computed: {
    meta() {
      return this.section.meta;
    },
    fields() {
      return this.hideNA
        ? this.section.fields.filter(this.isVisibleField)
        : this.section.fields;
    },
  },
  methods: {
    isVisibleField(field) {
      return !(
        (
          field.value === undefined || // emtpy value
          field.value === null ||
          (Array.isArray(field.value) && field.value.length === 0) || // empty array
          (field.value &&
            Object.keys(field.value).length === 0 &&
            Object.getPrototypeOf(field.value) === Object.prototype)
        ) // empty object
      );
    },
  },
};
</script>
