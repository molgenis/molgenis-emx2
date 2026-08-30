<script setup lang="ts">
import { useIntersectionObserver } from "@vueuse/core";
import { ref, type ComponentPublicInstance } from "vue";
import type { cellPayload } from "../../../types/types";
import type { RecordBox } from "../../utils/groupRecordSections";
import ContentBlock from "../content/ContentBlock.vue";
import DefinitionList from "../DefinitionList.vue";
import DefinitionListDefinition from "../DefinitionListDefinition.vue";
import DefinitionListTerm from "../DefinitionListTerm.vue";
import ValueEMX2 from "../value/EMX2.vue";

const props = withDefaults(
  defineProps<{
    section: RecordBox;
    trackInView?: boolean;
  }>(),
  {
    trackInView: false,
  }
);

const emit = defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
  (e: "inView"): void;
}>();

const root = ref<ComponentPublicInstance | null>(null);

// RefSelect renders one of these per option, so an untracked box builds no observer at all.
if (props.trackInView) {
  // A record box is often taller than the viewport, so the band is its top fifth rather than half
  // of it, which a tall box can never reach.
  useIntersectionObserver(
    root,
    ([entry]) => {
      if (entry?.isIntersecting) {
        emit("inView");
      }
    },
    { root: null, rootMargin: "0px 0px -80% 0px", threshold: 0 }
  );
}
</script>

<template>
  <!-- No `title`, because a heading box needs an h3 where ContentBlock emits an h2. -->
  <ContentBlock ref="root" :id="section.id" class="scroll-mt-7.5">
    <h2
      v-if="section.label && section.kind === 'section'"
      class="mb-5 uppercase text-heading-4xl font-display"
    >
      {{ section.label }}
    </h2>
    <h3 v-else-if="section.label" class="mb-5 text-heading-4xl font-display">
      {{ section.label }}
    </h3>

    <DefinitionList v-if="section.fields.length" :compact="false">
      <template v-for="field in section.fields" :key="field.id">
        <DefinitionListTerm>{{ field.label }}</DefinitionListTerm>
        <DefinitionListDefinition>
          <ValueEMX2
            :metadata="field.metadata"
            :data="field.value"
            @valueClick="$emit('valueClick', $event)"
          />
        </DefinitionListDefinition>
      </template>
    </DefinitionList>
  </ContentBlock>
</template>
