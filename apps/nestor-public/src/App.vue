<script setup lang="ts">
import { ref } from "vue";
//@ts-expect-error
import { Molgenis } from "molgenis-components";
//@ts-expect-error
import { AppFooter } from "molgenis-viz";

const session = ref(null);
const page = ref(null);
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
    <AppFooter
      id="project-foooter"
      first-column-title="KWF Nestor"
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
          <router-link :to="{ name: 'contact' }">Contact Us</router-link>
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
        <li id="project-funding-logo">
          <img
            src="/img/kwf_supported_by_logo.png"
            class="logo logo-small"
            alt="NESTOR Registry funded by the KWF"
          />
        </li>
        <li id="project-logo-link">
          <img
            src="/img/nestor-logo.png"
            class="logo logo-small"
            alt="The Nestor Registry"
          />
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
