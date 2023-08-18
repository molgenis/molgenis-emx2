<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
    <AppFooter />
  </Molgenis>
</template>

<script setup>
import { Molgenis } from "molgenis-components";
import { ref } from "vue";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);
</script>
