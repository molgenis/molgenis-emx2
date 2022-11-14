<script setup>
import Container from "./../Container.vue";
</script>

<template>
  <Container>
    <slot name="header"></slot>
    <div class="flex gap-7.5">
      <aside class="hidden w-95 md:block">
        <slot name="side"></slot>
      </aside>

      <div>
        <slot name="main"></slot>
      </div>
    </div>
  </Container>
</template>
