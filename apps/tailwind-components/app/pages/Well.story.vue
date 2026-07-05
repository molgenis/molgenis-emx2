<template>
  <Story title="Well" :spec="spec">
    <div class="flex flex-wrap gap-4 p-6">
      <Well>Default</Well>
      <Well>
        <b>With some html</b>
      </Well>
    </div>
  </Story>
</template>

<script setup>
import Well from "../components/Well.vue";

const spec = `
## Features
- Presentational pill/badge component for labels and tags
- Uses theme-aware filter tokens (bg-button-filter, text-button-filter, rounded-input)
- Non-interactive — wrap in Button for click behavior
- Slot-based content

## Props
None — pure slot component

## Test Checklist
- [ ] Renders across all 5 themes (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Text content displays correctly
- [ ] Rounded-input applies theme-specific border radius
- [ ] Colors match filterWell button variant
`;
</script>
