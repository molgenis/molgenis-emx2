<script lang="ts" setup>
import { ref } from "vue";
// @ts-expect-error
import { Molgenis } from "molgenis-components";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page"></router-view>
    <AppFooter />
  </Molgenis>
</template>
