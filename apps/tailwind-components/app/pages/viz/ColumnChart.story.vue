<script lang="ts" setup>
import { ref } from "vue";
import ColumnChart from "../../components/viz/ColumnChart/ColumnChart.vue";

const data = [
  { name: "Group A", value: 42 },
  { name: "Group B", value: 31 },
  { name: "Group C", value: 82 },
  { name: "Group D", value: 3 },
];
</script>

<template>
  <div class="h-auto grid gap-2.5 md:grid-cols-3">
    <div class="col-span-2">
      <ColumnChart
        id="column-chart-demo"
        title="Participants by experimental group"
        description="Participants (n=158) were randomised into one of four groups"
        :data="data"
        xvar="name"
        yvar="value"
        x-axis-label="Experimental group"
        y-axis-label="Number of participants"
      />
    </div>
    <div></div>
  </div>
</template>
