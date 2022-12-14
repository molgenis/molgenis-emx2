<script setup lang="ts">
const { tree } = defineProps<{
  tree: IOntologyNode[];
}>()
</script>

<template>
  <ul class="text-body-base" >
    <ContentTreeNode v-for="node in tree" :node="node"/>
  </ul>
</template>
