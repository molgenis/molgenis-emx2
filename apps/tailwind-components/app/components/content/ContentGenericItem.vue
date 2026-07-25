<script setup lang="ts">
import { computed } from "vue";
import type { ISectionField, cellPayload } from "../../../types/types";
import ValueEMX2 from "../value/EMX2.vue";
import ContentTypeHyperLink from "./type/ContentTypeHyperLink.vue";
import ContentTypeOntologyArray from "./type/ContentTypeOntologyArray.vue";
import ContentTypeRefBack from "./type/ContentTypeRefBack.vue";
import ContentTypeString from "./type/ContentTypeString.vue";
import ContentTypeText from "./type/ContentTypeText.vue";

const { field } = defineProps<{
  field: ISectionField;
}>();

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const contentTypeComponent = computed(() => {
  switch (field.meta?.columnType) {
    case "TEXT":
      return ContentTypeText;
    case "STRING":
      return ContentTypeString;
    case "ONTOLOGY_ARRAY":
      return ContentTypeOntologyArray;
    case "HYPERLINK":
      return ContentTypeHyperLink;
    case "REFBACK":
      return ContentTypeRefBack;
    default:
      return null;
  }
});
</script>

<template>
  <div class="grid md:grid-cols-3 md:gap-2.5">
    <dt class="flex items-start font-bold text-body-base capitalize">
      {{ field.meta?.label || field.meta?.id }}
    </dt>

    <dd class="col-span-2">
      <component
        :is="contentTypeComponent"
        v-if="contentTypeComponent"
        :field="field"
      />
      <ValueEMX2
        v-else
        :metadata="field.meta"
        :data="field.value"
        @valueClick="$emit('valueClick', $event)"
      />
    </dd>
  </div>
</template>
