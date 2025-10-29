<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";

withDefaults(
  defineProps<{
    name: string;
    link: string;
    icon?: string;
    external?: boolean;
  }>(),
  {
    external: true
  }
);
</script>

<template>
  <a :href="link" class="underline" :target="external ? '_blank' : '_self'" :rel="external ? 'external noopener noreferrer': ''">
    <BaseIcon class="inline" name="icon" :width="16" v-if="icon" /><span>{{ name }}</span><BaseIcon class="inline" name="ExternalLink" :width="16" v-if="external" />
  </a>
</template>
