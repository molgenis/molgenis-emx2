<template>
  <Molgenis id="__top" v-model="session">
    <div>
      <router-view :session="session" :page="page" />
      <AppFooter />
    </div>
  </Molgenis>
</template>

<script setup>
import { ref } from "vue";
import { Molgenis } from "molgenis-components";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);
</script>
