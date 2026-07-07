<script setup lang="ts">
import { ref } from "vue";
// @ts-ignore
import { Molgenis } from "molgenis-components";
// @ts-ignore
import { AppFooter } from "molgenis-viz";

const session = ref(null);
const page = ref(null);
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
      <AppFooter id="ernSkinFooter"
      first-column-title="ERN CRANIO"
      second-column-title="For members">
        <template #column-links-1>
          <li>
            <router-link :to="{ name: 'home' }"> Home </router-link>
          </li>
          <li>
            <router-link :to="{ name: 'about' }"> About </router-link>
          </li>
          <li>
            <router-link :to="{ name: 'dashboard' }"> Dashboard </router-link>
          </li>
        </template>
        <template #column-links-2>
          <li>
            <router-link :to="{ name: 'providers' }">
              View Providers
            </router-link>
          </li>
          <li>
            <router-link :to="{ name: 'documents' }"> Documents </router-link>
          </li>
        </template>
        <template #column-logos>
          <li id="project-logo-link">
            <router-link :to="{ name: 'home' }">
              <img
                src="/img/ern-cranio-logo.png"
                alt="ERN CRANIO: European Reference Network for rare and/or complex craniofacial anomalies and ear, nose and throat (ENT) disorders"
              />
            </router-link>
          </li>
        </template>
      </AppFooter>
  </Molgenis>
</template>
