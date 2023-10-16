<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Data Highlights Example"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'data-highlights' }">
            Data Highlights
          </router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection :verticalPadding="2">
      <h2>Data Highlights Component</h2>
      <p>
        The <strong>DataHighlights</strong> component is used to showcase
        interesting or important values. Ideally, this component should be
        rendered at the top of a page (e.g., dashboard) or can be restyled using
        CSS to fit your needs.
      </p>
      <p>
        The input data is an object containing one or more key-value pairs. It
        is recommended to limit the number of values to five or less as it
        defeats the purpose of highlighting key findings. If you need more,
        consider using the DataTable component.
      </p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading && !error">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && error" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <DataHighlights :data="summarised" v-else />
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { request } from "graphql-request";
import gql from "graphql-tag";
import { rollups } from "d3";
const d3 = { rollups };

import { asDataObject } from "../utils/utils.js";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import DataHighlights from "../components/viz/DataHighlights.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import headerImage from "../assets/highlights-header.jpg";

let loading = ref(false);
let error = ref(null);
let summarised = ref({});

async function getOrganisations() {
  const query = gql`
    {
      Organisations {
        name
        organisationType
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const data = d3
    .rollups(
      response.Organisations,
      (row) => row.length,
      (row) => row.organisationType
    )
    .map((group) => new Object({ type: group[0], count: group[1] }))
    .sort((a, b) => (a.type < b.type ? -1 : 1));
  summarised.value = asDataObject(data, "type", "count");
}

onMounted(() => {
  getOrganisations()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
.data-highlights {
  .data-highlight {
    background-color: $blue-900;
  }
}
</style>
