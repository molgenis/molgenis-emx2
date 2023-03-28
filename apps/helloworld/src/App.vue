<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
  </Molgenis>
</template>

<script setup>
import { Molgenis } from "molgenis-components";
import { ref } from "vue";

const session = ref(null);
const page = ref(null);
</script>
