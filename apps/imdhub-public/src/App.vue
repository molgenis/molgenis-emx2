<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
    <template #footer>
      <AppFooter />
    </template>
  </Molgenis>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { RouterView } from "vue-router";
import { Molgenis } from "molgenis-components";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);
</script>
