<script setup>
import Container from "./../Container.vue";
</script>

<template>
  <Container>
    <slot name="header"></slot>
    <div class="xl:flex xl:items-start">
      <aside class="xl:w-82.5 sticky top-[30px] flex-shrink-0 hidden xl:block">
        <slot name="side"></slot>
      </aside>

      <div class="xl:pl-7.5 grow">
        <slot name="main"></slot>
      </div>
    </div>
  </Container>
</template>
