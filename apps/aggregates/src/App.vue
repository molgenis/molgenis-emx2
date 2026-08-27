<script setup lang="ts">
import { ref } from "vue";
// @ts-ignore
import { Molgenis } from "molgenis-components";

const session = ref(null);
const page = ref(null);
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page"></router-view>
  </Molgenis>
</template>
