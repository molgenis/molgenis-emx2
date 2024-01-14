<template>
  <div>
    <h1>Hello World</h1>
    <ButtonAction @click="plusOne">{{ count }}</ButtonAction>
  </div>
</template>

<script setup>
import { ButtonAction } from "molgenis-components";
import { ref } from "vue";

let count = ref(0);

function plusOne() {
  count.value++;
}
</script>
