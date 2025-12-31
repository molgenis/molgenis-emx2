<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
    <AppFooter
      id="ernSkinFooter"
      first-column-title="ERN-SKin"
      second-column-title="For members"
      :show-project-citation="true"
    >
      <template v-slot:column-links-1>
        <li><router-link :to="{ name: 'home' }">Home</router-link></li>
        <li><router-link :to="{ name: 'about' }">About Us</router-link></li>
        <li>
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
        </li>
      </template>
      <template v-slot:column-links-2>
        <li>
          <router-link :to="{ name: 'documents' }">Documents</router-link>
        </li>
      </template>
      <template v-slot:column-logos>
        <li id="project-logo-link">
          <a href="https://ern-skin.eu">
            <img
              src="/img/ern-skin-logo.png"
              alt="ERN-Skin"
              class="ern-skin-logo"
            />
          </a>
        </li>
        <li class="eu-logos">
          <img src="/img/ern-logo.png" class="logo ern-logo" />
          <img
            src="/img/ern-skin-funding.png"
            class="logo funding-logo"
            alt="funded by the European Union"
          />
        </li>
        <li></li>
      </template>
    </AppFooter>
  </Molgenis>
</template>

<script setup lang="ts">
// @ts-ignore
import { Molgenis } from "molgenis-components";
import AppFooter from "./components/AppFooter.vue";
import { ref } from "vue";

const session = ref(null);
const page = ref(null);
</script>
