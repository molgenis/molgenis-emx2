<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style lang="scss">
body {
  padding: 0;
  margin: 0;
}

.app-page {
  font-family: Arial, Helvetica, sans-serif;
  font-size: 14pt;
  
  .bkg-light {
    background-color: $gray-050;
  }
  
  .output {
    display: block;
    width: 100%;
    box-sizing: content-box;
    padding: 1em;
    background-color: $gray-700;
    color: $gray-050;
  }

}

.message-box {
  .message-text {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    p {
      margin: 0;
    }
  }
}
</style>