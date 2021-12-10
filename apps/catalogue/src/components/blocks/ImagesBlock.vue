<template>
  <div class="">
    <span class="mr-3" v-for="(item, index) in items" :key="index">
      <span class="font-weight-bold pr-1">{{ item.label }}</span>
      <span v-show="item.text" class="">{{ item.text }}</span>
      <a :href="item.href">{{ item.href }}</a>
    </span>
  </div>
</template>

<script>
export default {
  name: "ImagesBlock",
  props: {
    items: {
      type: Array,
      required: false,
      default: () => [],
    },
  },
};
</script>

<style></style>
