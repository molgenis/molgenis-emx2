<template>
  <div>
    <i
      class="fa fa-question-circle text-primary"
      @mouseenter="show = true"
      @mouseleave="show = false"
    ></i>
    <div class="tooltip bs-tooltip-top" :class="{ show: show }">
      <span class="tooltip-inner">
        <span> <slot /></span>
      </span>
      <span class="arrow" />
    </div>
  </div>
</template>

<style>
.tooltip {
  margin-top: -50px;
}
</style>

<script>
export default {
  name: "Info",
  data() {
    return {
      show: false,
    };
  },
};
</script>

<docs>
<template>
  <demo-item>
    <Info>This is an info popover component.</Info>
  </demo-item>
</template>
</docs>
