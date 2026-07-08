<script setup lang="ts">
import {
  getBorderRadiusRecords,
  getBoxShadowRecords,
} from "../../utils/designTokens";

const radiusRecords = getBorderRadiusRecords();
const shadowRecords = getBoxShadowRecords();
</script>

<template>
  <div class="space-y-10">
    <h1 class="text-heading-3xl text-title">Radius & elevation</h1>

    <section aria-labelledby="radius-heading">
      <h2 id="radius-heading" class="text-heading-2xl text-title mb-6">
        Border radius
      </h2>
      <div class="flex flex-wrap gap-6">
        <div
          v-for="record in radiusRecords"
          :key="record.tokenName"
          class="flex flex-col items-center gap-2"
        >
          <div
            class="w-24 h-24 bg-button-primary border-2 border-button-primary"
            :class="record.tokenName"
            :title="`rounded-${record.label}`"
            aria-hidden="true"
          ></div>
          <TokenLabel :token-name="record.tokenName" />
          <span class="text-body-xs text-disabled font-mono">
            {{ record.cssVar }}
          </span>
        </div>
      </div>
    </section>

    <section aria-labelledby="elevation-heading">
      <h2 id="elevation-heading" class="text-heading-2xl text-title mb-6">
        Elevation (box shadows)
      </h2>
      <div class="flex flex-wrap gap-8">
        <div
          v-for="record in shadowRecords"
          :key="record.tokenName"
          class="flex flex-col items-center gap-2"
        >
          <div
            class="w-32 h-20 bg-content rounded-theme"
            :class="record.tokenName"
            :title="record.label"
            aria-hidden="true"
          ></div>
          <TokenLabel :token-name="record.tokenName" />
          <span
            class="text-body-xs text-disabled font-mono text-center max-w-32 break-words"
          >
            {{ record.cssVar }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>
