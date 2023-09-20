<template>
  <Page>
    <PageHeader
      title="ERN CRANIO Registry"
      :subtitle="`${orgName}`"
      :imageSrc="orgImageUrl"
    />
    <Dashboard class="provider-dashboard-container">
      <ProviderSidebar
        :userName="userName"
        :orgId="orgId"
        :orgName="orgName"
        :orgImageUrl="orgImageUrl"
      />
      <router-view
        :userName="userName"
        :orgId="orgId"
        :orgName="orgName"
        :orgImageUrl="orgImageUrl"
      >
      </router-view>
    </Dashboard>
  </Page>
</template>

<script setup>
import { Page, PageHeader, Dashboard } from "molgenis-viz";
import ProviderSidebar from "../components/ProviderSidebar.vue";

import viewProps from "../data/props";
const props = defineProps(viewProps);

</script>
