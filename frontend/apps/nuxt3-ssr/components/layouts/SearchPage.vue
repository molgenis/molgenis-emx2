<script setup>
import Container from "./../Container.vue";
</script>

<template>
  <Container>
    <slot name="header"></slot>
    <div class="xl:flex">
      <aside class="xl:min-w-95 xl:w-95 hidden xl:block">
        <slot name="side"></slot>
      </aside>
      <div class="xl:pl-7.5 xl:grow">
        <slot name="main"></slot>
      </div>
    </div>
  </Container>
</template>
