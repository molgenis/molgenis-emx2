<template>
  <ul v-if="documentation">
    <li v-for="d in documentation">
      <a v-if="d.url" :href="d.url">{{ d.name }}</a>
      <a v-else-if="d.file" :href="d.file.url">{{ d.name }}</a>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>

<script>
export default {
  props: {
    documentation: Array,
  },
};
</script>
