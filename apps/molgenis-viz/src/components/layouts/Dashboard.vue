<template>
  <div class="dashboard-container">
    <div class="dashboard-content">
      <slot></slot>
    </div>
  </div>
</template>

<style lang="scss">
.dashboard-container {
  background-color: $gray-050;
  padding: 2em 0;

  .dashboard-content {
    width: 90%;
    margin: 0 auto;
    padding: 2em 0;
    gap: 2em;
  }
}
</style>
