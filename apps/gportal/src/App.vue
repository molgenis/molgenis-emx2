<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
    <AppFooter />
  </Molgenis>
</template>

<script setup lang="ts">
import { ref } from "vue";
// @ts-ignore
import { Molgenis } from "molgenis-components";
// @ts-ignore
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);
</script>
