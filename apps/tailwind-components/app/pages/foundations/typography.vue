<script setup lang="ts">
import { computed } from "vue";
import { getTypographyRecords } from "../../utils/designTokens";

const typographyRecords = getTypographyRecords();

const headingRecords = computed(() =>
  typographyRecords.filter((r) => r.category === "heading")
);

const bodyRecords = computed(() =>
  typographyRecords.filter((r) => r.category === "body")
);
</script>

<template>
  <div class="space-y-10">
    <h1 class="text-heading-3xl text-title">Typography</h1>

    <section aria-labelledby="heading-scale-label">
      <h2 id="heading-scale-label" class="text-heading-2xl text-title mb-6">
        Heading scale
      </h2>
      <div class="space-y-6">
        <div
          v-for="record in headingRecords"
          :key="record.tokenName"
          class="border-b border-theme pb-4"
        >
          <div class="flex items-baseline gap-4 flex-wrap mb-1">
            <span :class="record.tokenName" class="text-title font-bold">
              The quick brown fox
            </span>
          </div>
          <div class="flex gap-4 items-center flex-wrap mt-1">
            <TokenLabel :token-name="record.tokenName" />
            <span class="text-body-sm text-disabled font-mono">
              {{ record.sizeRem }} / {{ record.lineHeight }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <section aria-labelledby="body-scale-label">
      <h2 id="body-scale-label" class="text-heading-2xl text-title mb-6">
        Body scale
      </h2>
      <div class="space-y-6">
        <div
          v-for="record in bodyRecords"
          :key="record.tokenName"
          class="border-b border-theme pb-4"
        >
          <div class="flex items-baseline gap-4 flex-wrap mb-1">
            <span :class="record.tokenName" class="text-title">
              The quick brown fox jumps over the lazy dog
            </span>
          </div>
          <div class="flex gap-4 items-center flex-wrap mt-1">
            <TokenLabel :token-name="record.tokenName" />
            <span class="text-body-sm text-disabled font-mono">
              {{ record.sizeRem }} / {{ record.lineHeight }}
            </span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
