<template>
  <Page>
    <PageHeader
      title="molgenis-viz Settings"
      subtitle="Create and customize your dashboard"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection class="bg-gray-050" width="large">
      <h2>Design your dashboard</h2>
      <template v-if="loading">
        <MessageBox>
          <p>Loading...</p>
        </MessageBox>
      </template>
      <template v-if="error">
        <MessageBox type="error">
          {{ error }}
        </MessageBox>
      </template>
      <template v-else>
        <p>
          Using the form below, create new charts using the table and variable
          picker.
        </p>
        <form title="Create charts" ref="chartBuilderForm">
          <!-- <div v-if="!charts.length" class="padding-v-1 padding-h-1">
            <p style="margin: 0;">No chart configurations found.</p>
          </div>
          <NewChartSettings v-else
            :tables="schema.tables"
            v-for="chart in charts"
          /> -->
          <NewChartSettings :tables="schema.tables" />
        </form>
        <button id="addNewChart" @click="addNewChart">
          Add new chart
          <PlusCircleIcon class="heroicons" />
        </button>
      </template>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { PlusCircleIcon } from "@heroicons/vue/24/outline";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import PageForm from "../components/layouts/PageForm.vue";
import MessageBox from "../components/display/MessageBox.vue";
import NewChartSettings from "../app-components/settings_new_chart.vue";

import { query_schema_meta } from "../utils/index";
import { SchemaMeta } from "../utils/types";

let loading = ref(true);
let error = ref(false);
let schema = ref<SchemaMeta | null>({ name: null, tables: null });
let charts = ref<Array[]>([]);

let chartBuilderForm = ref<Element[]>(null);

function addNewChart(event: Element) {
  charts.value.push("newChart");
}

onMounted(() => {
  query_schema_meta()
    .then((data) => (schema.value = data))
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
#addNewChart {
  margin-top: 1em;
  @include styleButton($background-color: none, $color: $blue-800);
  &:hover,
  &:focus {
    filter: brightness(95%);
  }
}
</style>
