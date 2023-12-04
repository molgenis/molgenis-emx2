<template>
  <Story title="Text/Tooltip" :layout="{ type: 'grid', width: '65%' }">
    <Variant title="Default">
      <p>From the <TextTooltip description="Web Accessibility Initiative">WAI</TextTooltip>; accessed 2023-12-05</p>
      <blockquote class="border-l-2 p-2 mb-2">
        A tooltip is a popup that displays information related to an element when the element receives keyboard focus or the mouse hovers over it. It typically appears after a small delay and disappears when <kbd>Escape</kbd> is pressed or on mouse out.
      </blockquote>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import TextTooltip from "./TextTooltip.vue";
</script>