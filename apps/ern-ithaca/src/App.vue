<script lang="ts" setup>
import { ref } from "vue";

// @ts-expect-error
import { Molgenis } from "molgenis-components";

// @ts-expect-error
import { AppFooter } from "molgenis-viz";
import { ArrowTopRightOnSquareIcon } from "@heroicons/vue/20/solid";

const session = ref(null);
const page = ref(null);
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page"></router-view>
    <AppFooter
      id="ithacaFooter"
      first-column-title="ERN Ithaca"
      second-column-title="For members"
    >
      <template #column-links-1>
        <li>
          <router-link :to="{ name: 'home' }"> Home </router-link>
        </li>
        <li>
          <a
            href="https://ern-ithaca.eu/about-us/"
            target="_blank"
            rel="noopener"
          >
            About
            <ArrowTopRightOnSquareIcon class="heroicons" />
          </a>
        </li>
        <li>
          <router-link :to="{ name: 'dashboard' }"> Dashboard </router-link>
        </li>
        <li>
          <router-link :to="{ name: 'documents' }"> Documents </router-link>
        </li>
        <li>
          <a
            href="https://ern-ithaca.eu/our-research-activities/iliad/iliad-transparency/"
            target="_blank"
            rel="noopener"
          >
            Transparency Statement
            <ArrowTopRightOnSquareIcon class="heroicons" />
          </a>
        </li>

        <li>
          <a
            href="https://ern-ithaca.eu/our-research-activities/iliad/iliad-projects-publications/"
            target="_blank"
            rel="noopener"
          >
            Projects & Publications
            <ArrowTopRightOnSquareIcon class="heroicons" />
          </a>
        </li>
      </template>
      <template #column-links-2> </template>
      <template #column-logos>
        <li id="project-logo-link">
          <router-link :to="{ name: 'home' }">
            <!-- insert project logo here -->
            <img
              src="/img/ern-ithaca-logo.png"
              class="project-logo"
              alt="ERN Ithaca: an international library of intellectual disability and anomalies of development"
            />
          </router-link>
        </li>
      </template>
    </AppFooter>
  </Molgenis>
</template>
