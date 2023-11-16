<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
    <AppFooter
      id="genturisFooter"
      first-column-title="Genturis Registry"
      second-column-title="For members"
      :show-project-citation="true"
    >
      <template v-slot:column-links-1>
        <li>
          <router-link :to="{ name: 'home' }"> Home </router-link>
        </li>
        <li>
          <router-link :to="{ name: 'about' }">About Us</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'governance' }">Governance</router-link>
        </li>
      </template>
      <template v-slot:column-links-2>
        <li>
          <router-link :to="{ name: 'documents' }"> Documents</router-link>
        </li>
      </template>
      <template v-slot:column-logos>
        <li id="genturis-logo-link">
          <a href="">
            <img
              src="/genturis-registry-logo.png"
              class="genturis-logo logo-small"
              alt="Registry for the European Reference Network on genetic tumour risk syndromes"
            />
          </a>
        </li>
      </template>
      <template v-slot:site-citation>
        <li>
          <router-link :to="{ name: 'privacy' }">Privacy Policy</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'disclaimer' }">Disclaimer</router-link>
        </li>
      </template>
    </AppFooter>
  </Molgenis>
</template>

<script setup>
import { ref } from "vue";
import { Molgenis } from "molgenis-components";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);
</script>
