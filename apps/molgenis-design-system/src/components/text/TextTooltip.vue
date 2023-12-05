<template>
  <span ref="instance" class="relative z-0">
    <span
      ref="toolTipLabel"
      :aria-describedby="`tooltip-${id}`"
      class="underline underline-offset-4 cursor-pointer outline-none focus:text-blue-500"
      @mouseover="showTooltip"
      @mouseout="hideTooltip"
      @keyup.esc="hideTooltip"
      @focus="showTooltip"
      @blur="hideTooltip"
      tabindex="0"
    >
      <slot></slot>
    </span>
    <span 
      :id="`tooltip-${id}`"
      ref="tooltipText"
      role="tooltip"
      class="
        sr-only
        block
        w-[12rem]
        absolute
        top-7
        -left-10
        py-1
        px-3
        m-auto
        rounded-md
        bg-gray-900
        text-gray-200
        z-10

        before:block
        before:absolute
        before:content-['']
        before:w-6
        before:h-6
        before:rotate-45
        before:rounded-md
        before:bg-gray-900
        before:-top-1
        before:left-9
        before:-z-10
      "
    >
      {{ description }}
    </span>
  </span>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";

interface Props {
  description: string
}

defineProps<Props>();

const id = ref(String(Math.random()).slice(2,12));
const tooltipLabel = ref({});
const tooltipText = ref({});


function showTooltip () {
  setTimeout(() => {
    tooltipText.value.classList.remove('sr-only');
  }, 300)
}

function hideTooltip () {
  tooltipText.value.classList.add('sr-only');
}

onMounted(() => {
  
})

</script>