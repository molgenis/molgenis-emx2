<template>
  <div class="loading__block">
    <div
      v-if="loading"
      class="block__background"
      ref="loadingRef"
      :data-loading="loading"
    ></div>
    <div class="block__content" ref="contentRef">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUpdated } from "vue";

withDefaults(
  defineProps<{
    loading: boolean;
  }>(),
  {
    loading: true,
  }
);

const contentRef = ref<HTMLDivElement>();
const loadingRef = ref<HTMLDivElement>();

function setLoadingBlockHeight() {
  if (loadingRef.value) {
    (
      loadingRef.value as HTMLDivElement
    ).style.height = `${contentRef.value?.clientHeight}px`;
  }
}

onMounted(() => setLoadingBlockHeight());
onUpdated(() => setLoadingBlockHeight());
</script>

<style lang="scss">
@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}

.loading__block {
  position: relative;

  .block__background {
    position: absolute;
    background-color: $blue-green-200;
    opacity: 1;
    animation: pulse 2.2s linear infinite;
    width: 100%;
    z-index: 1;

    &[data-loading="true"] + .block__content {
      opacity: 0;
    }
  }

  .block__content {
    opacity: 1;
  }
}
</style>
