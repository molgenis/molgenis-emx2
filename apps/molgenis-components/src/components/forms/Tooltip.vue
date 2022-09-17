<template>
  <span>
    <div v-if="value && showTooltip" class="mg-tooltip">
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
  border-bottom: 1px dotted black;
}

.mg-tooltip .mg-tooltiptext {
  width: 240px;
  background-color: #555;
  color: #fff;
  text-align: center;
  border-radius: 6px;
  padding: 5px 0;
  position: absolute;
  z-index: 1;
  opacity: 1;
  transition: color 0.15s ease-in-out, background-color 0.15s ease-in-out,
    border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
  top: -5px;
  margin-left: -0px;
  left: 105%;
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
