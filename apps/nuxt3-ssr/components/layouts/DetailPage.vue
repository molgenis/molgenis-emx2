<script setup>
import Container from "./../Container.vue";
</script>

<template>
  <Container>
    <slot name="header"></slot>
    <div>
      <aside class="md:w-82.5 md:fixed">
        <slot name="side"></slot>
      </aside>

      <div class="md:ml-82.5 md:pl-7.5">
        <slot name="main"></slot>
      </div>
    </div>
  </Container>
</template>
