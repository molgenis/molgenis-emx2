<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    name: string;
    address: string[];
    email?: string;
    centerText?: boolean;
    showEmail?: boolean;
  }>(),
  {
    centerText: true,
    showEmail: true,
  }
);
</script>

<template>
  <address :class="{ 'text-center': centerText, 'text-left': !centerText }">
    <span class="font-weight-bold">{{ name }}<br /></span>
    <span v-for="item in address">{{ item }}<br /></span>
    <a v-if="showEmail && email" :href="`mailto:${email}`">
      {{ email }}
    </a>
  </address>
</template>
