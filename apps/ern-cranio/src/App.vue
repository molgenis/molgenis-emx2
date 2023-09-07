<template>
  <Molgenis id="__top" v-model="session">
    <router-view
      :session="session"
      :page="page"
      :user="user"
      :organization="org"
    />
    <AppFooter :organization="org" />
  </Molgenis>
</template>

<script setup>
import { Molgenis } from "molgenis-components";
import { ref } from "vue";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);

const user = ref("David");
const org = ref({
  id: "umcg",
  name: "UMCG",
  label: "University Medical Centre Groningen",
});
</script>
