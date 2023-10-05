<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Bar Chart Example"
      :imageSrc="headerImage"
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
        tablular format. The table is responsive and can be customised using
        CSS. All cells, rows, and columns can be selected using values in the
        data. Click events are also available.
      </p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <Datatable
        v-else
        tableId="institutionsTable"
        :data="data"
        caption="All Groningen-based institutions in ROR"
        :columnOrder="['name', 'city', 'iri']"
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
import { fetchData } from "../utils/utils.js";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import Datatable from "../components/viz/DataTable.vue";
import headerImage from "../assets/table-header.jpg";

let loading = ref(true);
let hasError = ref(false);
let error = ref(null);
let data = ref([]);
let selection = ref({});

const query = `{
  Organisations(
    filter: { city: { equals: "Groningen" } }
  ) {
    name
    city
    country
    ontologyTermURI
  }
}`;

function updateSelection(value) {
  selection.value = value;
}

onMounted(() => {
  Promise.resolve(fetchData("/api/graphql", query))
    .then((response) => {
      data.value = response.data.Organisations.map((row) => {
        return {
          ...row,
          iri: `<a href=${row.ontologyTermURI}>${row.ontologyTermURI}</a>`,
        };
      });
      loading.value = false;
    })
    .catch((error) => {
      hasError.value = true;
      error.value = error;
    });
});
</script>
