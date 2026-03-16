<script setup lang="ts">
import ProgressMeter from "../../components/viz/ProgressMeter/ProgressMeter.vue";

const data = [
  {
    group: "Hospital A",
    rateOfCompletion: 90,
    totalScore: 100,
    color: "#12B3EB",
  },
  {
    group: "Hospital B",
    rateOfCompletion: 12,
    totalScore: 100,
    color: "#2897F0",
  },
  {
    group: "Hospital C",
    rateOfCompletion: 40,
    totalScore: 100,
    color: "#3E7CF4",
  },
  {
    group: "Hospital D",
    rateOfCompletion: 60,
    totalScore: 100,
    color: "#5460F9",
  },
];

const palette = Object.fromEntries(data.map((row) => [row.group, row.color]));
const total = data.reduce((acc, row) => acc + row.rateOfCompletion, 0);
</script>

<template>
  <ProgressMeter
    id="progress-meter"
    title="Participant recruitment targets for Year 1"
    :description="`Current recruiment progress by participating institution (n=${total}; target: 400)`"
    :data="data"
    labels="group"
    values="rateOfCompletion"
    totals="totalScore"
    :show-values-as-percentages="true"
    :color-palette="palette"
  />
</template>
