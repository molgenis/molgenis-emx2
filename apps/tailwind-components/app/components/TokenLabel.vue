<script setup lang="ts">
import { ref, computed } from "vue";
import { useClipboard } from "@vueuse/core";
import Button from "./Button.vue";

const props = defineProps<{
  tokenName: string;
  ariaLabel?: string;
}>();

const { copy } = useClipboard({ legacy: true });
const copied = ref(false);

const buttonAriaLabel = computed(
  () => props.ariaLabel ?? `Copy token name ${props.tokenName}`
);

const copyIcon = computed(() => (copied.value ? "check" : "copy"));

async function copyToken() {
  await copy(props.tokenName);
  copied.value = true;
  setTimeout(() => {
    copied.value = false;
  }, 1500);
}
</script>

<template>
  <Button
    type="text"
    size="tiny"
    :icon="copyIcon"
    icon-position="right"
    :aria-label="buttonAriaLabel"
    class="font-mono"
    @click="copyToken"
  >
    <span class="text-sm">{{ tokenName }}</span>
    <span
      class="text-xs px-1 rounded transition-opacity"
      :class="copied ? 'opacity-100 text-valid' : 'opacity-0'"
      aria-live="polite"
    >
      Copied
    </span>
  </Button>
</template>
