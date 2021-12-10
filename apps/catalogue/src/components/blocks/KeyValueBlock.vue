<template>
  <grid-block :heading="computedHeading">
    <read-more
      v-if="items.length === 1 && items[0].value"
      :text="items[0].value"
      :length="750"
    />
    <div v-else-if="items.length > 1">
      <div v-for="(item, index) in items" :key="index">
        <strong>{{ item.label }}</strong>
        <p>{{ item.value }}</p>
      </div>
    </div>
  </grid-block>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";
import GridBlock from "./GridBlock.vue";

export default {
  name: "KeyValueBlock",
  components: { GridBlock, ReadMore },
  props: {
    heading: {
      type: String,
      required: false,
    },
    /**
     * List of items
     * Item has:
     * key: String (required)
     * label: String (required)
     * value: String, Number, Boolean, Object
     */
    items: {
      type: Array,
      required: false,
      default: () => [],
    },
  },
  computed: {
    computedHeading() {
      return this.heading
        ? this.heading
        : this.items.length === 1
        ? this.items[0].label
        : null;
    },
  },
};
</script>

<style></style>
