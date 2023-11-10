<template>
  <div class="dashboard-box">
    <slot></slot>
  </div>
</template>

<style lang="scss">
.dashboard-box {
  box-shadow: $box-shadow;
  box-sizing: content-box;
  border-radius: 8px;
  background-color: $gray-000;
}
</style>
