<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <table class="w-full table-auto">
    <caption>{{ caption }}</caption>
    <thead>
      <slot name="head"></slot>
    </thead>
    <tbody>
      <slot name="body"></slot>
    </tbody>
    <tfoot>
      <slot name="footer"></slot>
    </tfoot>
  </table>
</template>

<script setup lang="ts">
defineProps<{
  caption?: string
}>();
</script>