<template>
  <div class="tooltip-component">
    <slot> Default </slot>
    <div class="tooltip-text px-2">{{ text }}</div>
  </div>
</template>

<script>
export default {
  props: {
    text: {
      type: String,
      required: true,
    },
  },
};
</script>

<style scoped>
.tooltip-component {
  position: relative;
  width: auto;
  align-self: flex-start;
  justify-self: flex-start;
}

.tooltip-component:hover .tooltip-text {
  display: block;
}

.tooltip-text {
  display: none;
  background-color: var(--dark);
  color: var(--light);
  white-space: nowrap;
  position: absolute;
  right: 0px;
  z-index: 1040;
  border-radius: 4px;
}
</style>

<docs>
<template>
  <demo-item>
    <div><small><i>Used in the directory app.</i></small></div>
    <TooltipComponent :text="'I am a tooltip component that wraps round an element'">
     <div >Hover me!</div>
    </TooltipComponent>
  </demo-item>
</template>
</docs>
