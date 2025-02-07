<template>
  <div>
    <h1>Date</h1>
    <flat-pickr v-model="date" class="border bottom-1" />
    <p>Selected date: {{ date }}</p>
  </div>
</template>

<script setup>
import { ref } from "vue";
import flatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.css";

const date = ref(null);
</script>
