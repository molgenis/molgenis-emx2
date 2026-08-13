<script setup lang="ts">
import { ref } from "vue";
// @ts-ignore
import { Molgenis } from "molgenis-components";
// @ts-expect-error
import { AppFooter } from "molgenis-viz";

const session = ref(null);
const page = ref(null);
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
    <AppFooter
      id="reconnetFooter"
      first-column-title="ERN ReCONNET"
      second-column-title="For members"
    >
      <template #column-links-1>
        <li>
          <router-link :to="{ name: 'home' }">Home</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'about-us' }">About Us</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'documents' }">Documents</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
        </li>
      </template>
      <template #column-logos>
        <li id="project-logo-link">
          <router-link :to="{ name: 'home' }">
            <img
              src="/img/ern-reconnet-logo.png"
              alt="ERN-RECONNET registry"
              class="ern-logo"
            />
          </router-link>
        </li>
        <li class="eu-logos">
          <img
            src="/img/ern-funded-by-eu.svg"
            class="logo funding-logo"
            alt="funded by the European Union"
          />
        </li>
      </template>
    </AppFooter>
  </Molgenis>
</template>
