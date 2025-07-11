<script setup lang="ts">
import type { Section } from "~/types/types";

defineProps<{
  section: Section;
}>();
</script>

<template>
  <section
    class="px-8 first:pt-[50px] last:pb-[50px]"
    :class="section.heading ? 'pt-[50px]' : ''"
  >
    <h3
      v-if="section.heading"
      class="text-heading-3xl font-display text-title-contrast mb-4"
    >
      {{ section.heading }}
    </h3>
    <DefinitionList :compact="false">
      <template v-for="field in section.fields">
        <DefinitionListTerm class="text-title-contrast"
          >{{ field.metadata.label }}
        </DefinitionListTerm>
        <DefinitionListDefinition class="text-title-contrast">
          <ValueEMX2
            :data="field.value"
            :metadata="field.metadata"
            @valueClick="$emit('valueClick', $event)"
          />
        </DefinitionListDefinition>
      </template>
    </DefinitionList>
  </section>
</template>
