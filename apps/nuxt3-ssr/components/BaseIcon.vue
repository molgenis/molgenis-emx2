<script setup>
defineProps({
  name: {
    type: String,
    required: true,
  },
  width: {
    type: Number,
    default: 24,
  },
});
const icons = {}
icons['arrow-down'] = resolveComponent('IconsArrowDown')
icons['arrow-left'] = resolveComponent('IconsArrowLeft')
icons['arrow-right'] = resolveComponent('IconsArrowRight')
icons['caret-down'] = resolveComponent('IconsCaretDown')
icons['caret-left'] = resolveComponent('IconsCaretLeft')
icons['caret-right'] = resolveComponent('IconsCaretRight')
icons['star'] = resolveComponent('IconsStar')
</script>

<template>
  <component :is="icons[name]" :width="width"></component>
</template>
