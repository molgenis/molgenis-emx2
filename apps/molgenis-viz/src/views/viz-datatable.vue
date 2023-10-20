<template>
  <Page>
    <PageHeader
      title="molgenis-viz"
      subtitle="Bar Chart Example"
      imageSrc="table-header.jpg"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'datatable' }">Datatable</router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>Datatable</h2>
      <p>
        The <strong>Datatable</strong> component can be used to display data in
        tablular format. The table is responsive and has a mobile friendly
        layout. Tables are minimally styled and can be customised using CSS. All
        cells, rows, and columns can be selected using values in the data.
        Content can also be rendered as HTML. Click and hover events are also
        available.
      </p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading & !error">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && error" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <Datatable
        v-else
        tableId="institutionsTable"
        :data="data"
        caption="Top 10 cities by total number of organisations"
        :columnOrder="['city', 'country', 'organisations']"
        :renderHtml="true"
        @row-clicked="updateSelection"
      />
    </PageSection>
    <PageSection>
      <p>Click a row in the table of above to display the row-level data</p>
      <output class="output">
        {{ selection }}
      </output>
    </PageSection>
  </Page>
</template>
<script setup>
import { ref, onMounted } from "vue";
import { request } from "graphql-request";
import gql from "graphql-tag";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import Datatable from "../components/viz/DataTable.vue";

import { flatRollup } from "d3";
const d3 = { flatRollup };

let loading = ref(true);
let error = ref(null);
let data = ref([]);
let selection = ref({});

async function getOrganisations() {
  const query = gql`
    {
      Organisations {
        name
        city
        country
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const organisations = response.Organisations;
  data.value = d3
    .flatRollup(
      organisations,
      (row) => row.length,
      (row) => row.city,
      (row) => row.country
    )
    .map((arr) => {
      return { city: arr[0], country: arr[1], organisations: arr[2] };
    })
    .sort((a, b) => (a.organisations < b.organisations ? 1 : -1))
    .slice(0, 5);
}

function updateSelection(value) {
  selection.value = value;
}

onMounted(() => {
  getOrganisations()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>
