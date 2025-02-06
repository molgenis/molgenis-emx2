<template>
  <div>
    <PieChart
      chartId="organisationsByType"
      title="Summary of organisation type"
      :description="`0 organisations are included in the dataset. The following chart shows the breakdown of oganisations by type.`"
      :chartData="{ Dogs: 49, Cats: 28, Birds: 23 }"
      :enableClicks="true"
      :chartHeight="250"
      :asDonutChart="true"
    />
    <EditBlock v-if="editMode" type="PieChart" @action="$emit('action', $event)">
      <InputLabel for="database-input-string">
        Database
      </InputLabel>
      <InputString id="database-input-string" label="database" v-model="database" />
    </EditBlock>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { PieChart } from "molgenis-viz";
import { ref } from "vue";
let database = ref("pet store");

withDefaults(
  defineProps<{
    content?: { html: string };
    editMode?: boolean;
  }>(),
  {
    editMode: false,
  }
);
const emit = defineEmits(["save", "action"]);

</script>
