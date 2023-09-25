<script setup lang="ts">
import { ISectionField } from "interfaces/types";
const ContentTypeString = resolveComponent("ContentTypeString");
const ContentTypeText = resolveComponent("ContentTypeText");
const ContentTypeOntologyArray = resolveComponent("ContentTypeOntologyArray");

const { field } = defineProps<{
  field: ISectionField;
}>();

const component = computed(() => {
  switch (field.meta?.columnType) {
    case "TEXT":
      return ContentTypeText;
    case "STRING":
      return ContentTypeString;
    case "ONTOLOGY_ARRAY":
      return ContentTypeOntologyArray;
    default:
      return ContentTypeString;
  }
});
</script>

<template>
  <div class="grid md:grid-cols-3 md:gap-2.5">
    <dt class="flex items-start font-bold text-body-base">
      <div class="flex items-center gap-1">
        {{ field.meta?.name }} {{ field.meta?.columnType }}
      </div>
    </dt>

    <dd class="col-span-2">
      <component :is="component" :field="field" />
    </dd>
  </div>
</template>
