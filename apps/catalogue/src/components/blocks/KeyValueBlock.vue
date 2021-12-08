<template>
  <div class="">
    <h5 class="w-100" v-if="heading">{{ heading }}</h5>
    <template v-if="items.length === 1">
      <h5 v-if="!heading">{{ items[0].label }}</h5>
      <read-more :text="items[0].value" :length="750" />
    </template>
    <div v-else>
      <div class="w-100" v-for="(item, index) in items" :key="index">
        <strong>{{ item.label }}</strong>
        <p>{{ item.value }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  name: "KeyValueBlock",
  components: { ReadMore },
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
      required: true,
    },
  },
};
</script>

<style></style>
