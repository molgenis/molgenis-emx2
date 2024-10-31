<template>
  <div>
    <PieChart
      chartId="organisationsByType"
      title="Summary of organisation type"
      :description="`${total} organisations are included in the dataset. The following chart shows the breakdown of oganisations by type.`"
      :chartData="{ Dogs: 49, Cats: 28, Birds: 23 }"
      :enableClicks="true"
      :chartHeight="250"
      :asDonutChart="true"
    />
    <EditBlock v-if="editMode" type="PieChart">
      <InputString label="database" v-model="database" />
    </EditBlock>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { PieChart } from "molgenis-viz";
// @ts-ignore
import { InputString } from "molgenis-components";

import EditBlock from "../EditBlock.vue";
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
</script>
