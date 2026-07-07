<script setup lang="ts">
import { ref } from "vue";
import { Molgenis } from "molgenis-components";
import { AppFooter } from "molgenis-viz";

const session = ref(null);
const page = ref(null);
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <div>
      <router-view :session="session" :page="page" />
      <AppFooter>
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
          <li>
            <a href="https://www.molgenis.org">
              <span class="visually-hidden">
                visit the molgenis website to learn more
              </span>
              <li>
                <img
                  src="/img/molgenis-logo-blue-text.png"
                  class="molgenis-logo"
                  alt="molgenis open source data platform"
                />
              </li>
            </a>
          </li>
        </template>
      </AppFooter>
    </div>
  </Molgenis>
</template>
