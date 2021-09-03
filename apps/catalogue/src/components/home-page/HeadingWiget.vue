<template>
  <main>
    <h1 class="text-center">{{ title }}</h1>
    <h3 class="text-center text-secondary" v-show="subTitle">
      {{ subTitle }}
    </h3>
  </main>
</template>

<script>
export default {
  name: "HeadingWiget",
  props: {
    title: {
      type: String,
      default: () => "My Title",
    },
    subTitle: {
      type: String,
      default: () => "My Subtitle",
    },
  },
};
</script>
