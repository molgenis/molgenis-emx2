<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
  </Molgenis>
</template>

<script setup lang="ts">
import { Molgenis } from "molgenis-components";
import { isEmpty } from "meta-data-utils";
import type { ISetting } from "meta-data-utils";
import { ref } from "vue";

const setting: ISetting = {
  key: "test",
  value: "test",
};

console.log("is setting empty : ", isEmpty(setting));

const session = ref(null);
const page = ref(null);
</script>
