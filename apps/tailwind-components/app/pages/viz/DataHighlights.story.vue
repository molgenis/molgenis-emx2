<script setup lang="ts">
import DataHighlights from "../../components/cms/dataHighlights/DataHighlights.vue";
</script>

<template>
  <div class="grid grid-cols-3 gap-2.5 mt-5">
    <DataHighlights
      id="highlights-patients"
      highlightLabel="Patients"
      :highlightValue="12"
    />
    <DataHighlights
      id="highlights-samples"
      highlightLabel="Samples"
      :highlightValue="28"
    />
    <DataHighlights
      id="highlights-experiments"
      highlightLabel="Experiments"
      :highlightValue="28"
    />
  </div>
</template>
