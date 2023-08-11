<template>
  <div style="height: 100%">
    <Layout
      :settings="getPageSettings()"
      @update="saveDashboard"
      :admin="IsAdmin()"
    />
  </div>
</template>

<script setup>
import { request, gql } from "graphql-request";
import { ref } from "vue";
import { useRoute } from "vue-router";
import Layout from "./Layouts/Layout.vue";

const route = useRoute();
const props = defineProps({
  session: Object,
});

let dashboard = ref({});

function getPageName() {
  return route.params?.page || "default";
}

function getPageSettings() {
  return dashboard.value[getPageName()];
}

function IsAdmin() {
  return props?.session?.email === "admin";
}

async function loadDashboard() {
  const response = await request("graphql", `{_settings{key, value}}`);
  dashboard.value = JSON.parse(
    response._settings.find((setting) => setting.key == "dashboard").value
  );
}

async function saveDashboard(value) {
  const createMutation = gql`
    mutation change($settings: [MolgenisSettingsInput]) {
      change(settings: $settings) {
        message
      }
    }
  `;

  const variables = {
    settings: {
      key: "dashboard",
      value: JSON.stringify({ ...dashboard.value, [getPageName()]: value }),
    },
  };

  await request("graphql", createMutation, variables).catch((e) => {
    console.error(e);
  });
  loadDashboard();
}

loadDashboard();
</script>

<style>
div#app > div:first-child > div:first-child > div:nth-child(2) {
  max-height: calc(100vh - 70px - 60px);
  height: calc(100vh - 70px - 60px);
  overflow: auto;
}
</style>
