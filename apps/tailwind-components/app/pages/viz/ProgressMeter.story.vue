<script setup lang="ts">
import ProgressMeter from "../../components/viz/ProgressMeter/ProgressMeter.vue";

const data = [
  { group: "Group A", rateOfCompletion: 90, totalScore: 100 },
  { group: "Group B", rateOfCompletion: 12, totalScore: 100 },
  { group: "Group C", rateOfCompletion: 40, totalScore: 100 },
  { group: "Group D", rateOfCompletion: 60, totalScore: 100 },
];
</script>

<template>
  <ProgressMeter
    id="progress-meter"
    title="Test"
    :data="data"
    labels="group"
    values="rateOfCompletion"
    totals="totalScore"
    :show-values-as-percentages="true"
    :color-palette="{
      'Group A': '#12B3EB',
      'Group B': '#2897F0',
      'Group C': '#3E7CF4',
      'Group D': '#5460F9',
    }"
  />
</template>
