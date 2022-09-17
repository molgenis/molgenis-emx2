<template>
  <span class="mg-tooltip-container">
    <slot />
    <div v-if="value" class="mg-tooltip">
      <div class="mg-tooltiptext">
        {{ value }}
      </div>
    </div>
  </span>
</template>

<style>
/** https://www.w3schools.com/css/css_tooltip.asp */
.mg-tooltip {
  position: relative;
  display: inline-block;
}

.mg-tooltip {
  opacity: 0;
  visibility: hidden;
}

.mg-tooltip .mg-tooltiptext {
  width: 240px;
  background-color: #555;
  color: #fff;
  text-align: center;
  border-radius: 6px;
  padding: 0;
  position: absolute;
  z-index: 1;
  top: -15px;
  margin-left: -0px;
  left: 105%;
}

.mg-tooltip-container:hover > .mg-tooltip {
  visibility: visible;
  opacity: 0.9;
}
</style>

<script>
export default {
  props: {
    /** the value of the tooltip. If left empty no tooltip is rendered */
    value: String,
  },
};
</script>

<docs>
<template>
  <demo-item>
    <Tooltip value="this is a tooltip">
      <IconAction icon="trash" @click="alert('clicked')"/>
    </Tooltip>
  </demo-item>
</template>
</docs>
