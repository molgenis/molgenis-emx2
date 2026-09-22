<script setup lang="ts">
import DefinitionList from "../../../../tailwind-components/app/components/DefinitionList.vue";
import DefinitionListDefinition from "../../../../tailwind-components/app/components/DefinitionListDefinition.vue";
import DefinitionListTerm from "../../../../tailwind-components/app/components/DefinitionListTerm.vue";
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
