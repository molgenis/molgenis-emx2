<script setup>
import Container from "./../Container.vue";
</script>

<template>
  <Container>
    <slot name="header"></slot>
    <aside class="xl:w-95 absolute hidden xl:block">
      <slot name="side"></slot>
    </aside>
    <div class="xl:ml-95 xl:pl-7.5">
      <slot name="main"></slot>
    </div>
  </Container>
</template>
