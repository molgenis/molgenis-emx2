<template>
  <div>
    <i
      class="fa fa-question-circle text-primary"
      @mouseover="show = true"
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
  data() {
    return {
      show: false,
    };
  },
};
</script>

<docs>
Example
```
<Info>This is some info</Info>
```
</docs>
