<script setup lang="ts">
import { computed } from "vue";
import {
  getPaletteColorRecords,
  getSemanticColorRecords,
} from "../../utils/designTokens";

const paletteRecords = getPaletteColorRecords();
const semanticRecords = getSemanticColorRecords();

const paletteFamilies = computed(() => {
  const familyMap = new Map<string, typeof paletteRecords>();
  for (const record of paletteRecords) {
    if (!familyMap.has(record.family)) familyMap.set(record.family, []);
    familyMap.get(record.family)!.push(record);
  }
  return familyMap;
});

const semanticByCategory = computed(() => {
  const backgroundRecords = semanticRecords.filter(
    (r) => r.category === "background"
  );
  const textRecords = semanticRecords.filter((r) => r.category === "text");
  const borderRecords = semanticRecords.filter((r) => r.category === "border");
  return { backgroundRecords, textRecords, borderRecords };
});
</script>

<template>
  <div class="space-y-10">
    <h1 class="text-heading-3xl text-title">Colors</h1>

    <section aria-labelledby="palette-heading">
      <h2 id="palette-heading" class="text-heading-2xl text-title mb-6">
        Palette
      </h2>
      <div
        v-for="[family, records] in paletteFamilies"
        :key="family"
        class="mb-8"
      >
        <h3 class="text-heading-xl text-title mb-3 capitalize">{{ family }}</h3>
        <ol class="flex flex-wrap gap-4">
          <li v-for="record in records" :key="record.tokenName">
            <ColorTile
              :color="`${record.family}-${record.shade}`"
              type="background"
            />
            <TokenLabel :token-name="record.tokenName" />
          </li>
        </ol>
      </div>
    </section>

    <section aria-labelledby="semantic-bg-heading">
      <h2 id="semantic-bg-heading" class="text-heading-2xl text-title mb-6">
        Semantic — Background
      </h2>
      <ol class="flex flex-wrap gap-4">
        <li
          v-for="record in semanticByCategory.backgroundRecords"
          :key="record.tokenName"
        >
          <ColorTile
            :color="record.tokenName.replace('bg-', '')"
            type="background"
          />
          <TokenLabel :token-name="record.tokenName" />
        </li>
      </ol>
    </section>

    <section aria-labelledby="semantic-text-heading">
      <h2 id="semantic-text-heading" class="text-heading-2xl text-title mb-6">
        Semantic — Text
      </h2>
      <ol class="flex flex-wrap gap-4">
        <li
          v-for="record in semanticByCategory.textRecords"
          :key="record.tokenName"
        >
          <ColorTile
            :color="record.tokenName.replace('text-', '')"
            type="text"
          />
          <TokenLabel :token-name="record.tokenName" />
        </li>
      </ol>
    </section>

    <section aria-labelledby="semantic-border-heading">
      <h2 id="semantic-border-heading" class="text-heading-2xl text-title mb-6">
        Semantic — Border
      </h2>
      <ol class="flex flex-wrap gap-4">
        <li
          v-for="record in semanticByCategory.borderRecords"
          :key="record.tokenName"
        >
          <ColorTile
            :color="record.tokenName.replace('border-', '')"
            type="border"
          />
          <TokenLabel :token-name="record.tokenName" />
        </li>
      </ol>
    </section>
  </div>
</template>
