<template>
  <div class="row">
    <span class="mr-3" v-for="(item, index) in items" :key="index">
      <span class="font-weight-bold pr-1">{{ item.label }}</span>
      <span v-show="item.text" class="">{{ item.text }}</span>
      <a :href="item.href">{{ item.href }}</a>
    </span>
  </div>
</template>

<script>
export default {
  name: "LinksBlock",
  props: {
    /**
     * List of link items
     * Link item has:
     * label: String (required)
     * href: String (required)
     * text: String
     */
    items: {
      type: Array,
      required: true,
    },
  },
};
</script>

<style></style>
