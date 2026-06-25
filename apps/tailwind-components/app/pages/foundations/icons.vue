<script setup lang="ts">
import BaseIcon from "../../components/BaseIcon.vue";
import { getIconNames } from "../../utils/designTokens";

const iconNames = getIconNames();
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-heading-3xl text-title">Icons</h1>

    <p class="text-body-base text-title">
      All available icon components. Use with
      <code class="font-mono">&lt;BaseIcon name="IconName" /&gt;</code>. Click a
      name to copy it.
    </p>

    <div
      class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6"
      role="list"
      aria-label="Icon library"
    >
      <div
        v-for="name in iconNames"
        :key="name"
        class="flex flex-col items-center gap-2 p-3 border border-theme rounded-theme hover:bg-hover transition-colors"
        role="listitem"
      >
        <BaseIcon :name="name" :width="32" class="text-title" />
        <TokenLabel :token-name="name" :aria-label="`Copy icon name ${name}`" />
      </div>
    </div>
  </div>
</template>
