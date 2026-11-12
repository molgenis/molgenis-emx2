<script setup lang="ts">
// @ts-ignore
import { Address } from "molgenis-viz";

withDefaults(defineProps<{ showEmail?: boolean; centerText?: boolean }>(), {
  showEmail: false,
  centerText: true,
});
</script>

<template>
  <Address
    name="University Medical Centre Groningen"
    :address="[
      'NESTOR Registry',
      'Department of Medical Genetics',
      'Antonius Deusinglaan 1, 9713 AV Groningen',
      'The Netherlands',
    ]"
    :showEmail="showEmail"
    email="support@molgenis.org"
    :centerText="centerText"
  />
</template>
