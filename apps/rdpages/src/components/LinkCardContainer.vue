<template>
  <div class="quicklink-cards">
    <slot></slot>
  </div>
</template>

<style lang="scss">
.quicklink-cards {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;

  .link-card {
    height: 10em;
    flex-grow: 1;
    
    span {
      @include textTransform(bold);
      font-size: 14pt;
    }
  }
}
</style>
