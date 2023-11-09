<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />

    <h3>{{ msg }}</h3>
  </Molgenis>
</template>

<script setup lang="ts">
import { Molgenis } from "molgenis-components";
import { sayHello } from "meta-data-utils";
import { ref } from "vue";

const msg = sayHello("World");

const session = ref(null);
const page = ref(null);
</script>
