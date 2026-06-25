<script setup lang="ts">
import { getSpacingRecords } from "../../utils/designTokens";

const spacingRecords = getSpacingRecords();
</script>

<template>
  <div class="space-y-8">
    <h1 class="text-heading-3xl text-title">Spacing</h1>

    <p class="text-body-base text-title">
      Custom spacing tokens from the theme. The key (e.g.
      <code class="font-mono">18</code>) can be used as
      <code class="font-mono">p-18</code>, <code class="font-mono">m-18</code>,
      <code class="font-mono">gap-18</code>, etc.
    </p>

    <div class="space-y-3" role="list" aria-label="Spacing tokens">
      <div
        v-for="record in spacingRecords"
        :key="record.tokenName"
        class="flex items-center gap-4"
        role="listitem"
      >
        <div
          class="bg-button-primary shrink-0 h-4"
          :style="{ width: record.cssValue }"
          :title="record.cssValue"
          aria-hidden="true"
        ></div>
        <TokenLabel :token-name="record.tokenName" />
        <span class="text-body-sm text-disabled font-mono">
          {{ record.cssValue }}
        </span>
      </div>
    </div>
  </div>
</template>
