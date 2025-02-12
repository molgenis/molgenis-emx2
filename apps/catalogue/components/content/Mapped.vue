<script setup lang="ts">
const { mapped } = defineProps<{
  mapped: { source: { name: string }; target: { name: string } }[];
}>();
</script>
<template>
  <ul class="text-body-base">
    <li v-for="mapping in mapped">
      <DefinitionList>
        <DefinitionListTerm>Source</DefinitionListTerm>

        <DefinitionListDefinition>{{
          mapping.source.name
        }}</DefinitionListDefinition>
        <DefinitionListTerm>Target</DefinitionListTerm>

        <DefinitionListDefinition>{{
          mapping.target.name
        }}</DefinitionListDefinition>
      </DefinitionList>
    </li>
  </ul>
</template>
