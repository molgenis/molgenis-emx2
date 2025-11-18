<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import {computed} from "vue";

const props = withDefaults(
    defineProps<{
      to: string;
      label: string;
      icon?: string;
      type?: "nuxt" | "static" | "external";
      inline?: boolean;
    }>(),
    {
      type: "external",
      inline: true,
    }
);

const external = computed(() => {
  return props.type === "external"
})

const target = computed(() => {
  return external ? "_blank" : "_self";
})

const rel = computed(() => {
  return external ? "external noopener noreferrer" : "";
})
</script>

<template>
  <NuxtLink :to="to" class="underline" :class="inline ? 'ml-1' : ''" :target="target" :rel="rel" :external="external">
    <BaseIcon class="inline" name="icon" :width="16" v-if="icon" /><span>{{ label }}</span><BaseIcon class="inline" name="ExternalLink" :width="16" v-if="external" />
  </NuxtLink>
</template>