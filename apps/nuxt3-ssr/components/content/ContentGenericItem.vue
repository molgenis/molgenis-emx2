<script setup lang="ts">
import { ISectionField } from "~/interfaces/types";
const String = resolveComponent("ContentTypeString");
const Text = resolveComponent("ContentTypeText");
const OntologyArray = resolveComponent("ContentTypeOntologyArray");
const HyperLink = resolveComponent("ContentTypeHyperLink");

const { field } = defineProps<{
  field: ISectionField;
}>();

const component = computed(() => {
  switch (field.meta?.columnType) {
    case "TEXT":
      return Text;
    case "STRING":
      return String;
    case "ONTOLOGY_ARRAY":
      return OntologyArray;
    case "HYPERLINK":
      return HyperLink;
    default:
      return String;
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
