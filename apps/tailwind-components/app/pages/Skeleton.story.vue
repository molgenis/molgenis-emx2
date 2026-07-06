<template>
  <Story title="Skeleton" :spec="spec">
    <div class="p-6 space-y-8 max-w-sm">
      <div>
        <h3 class="text-record-heading mb-2">Default (4 lines)</h3>
        <Skeleton />
      </div>
      <div>
        <h3 class="text-record-heading mb-2">Custom (2 lines)</h3>
        <Skeleton :lines="2" />
      </div>
      <div>
        <h3 class="text-record-heading mb-2">Many (8 lines)</h3>
        <Skeleton :lines="8" />
      </div>
    </div>
  </Story>
</template>

<script setup>
import Skeleton from "../components/Skeleton.vue";

const spec = `
## Features
- Reusable skeleton loading placeholder
- Theme-aware background color
- Configurable number of lines
- Animated pulse effect
- Accessible role="status"

## Props
| Prop | Type | Default |
|------|------|---------|
| lines | number | 4 |

## Test Checklist
- [ ] Renders across all 5 themes
- [ ] Animation visible
- [ ] Bar widths vary progressively
`;
</script>
