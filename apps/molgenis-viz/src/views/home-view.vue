<template>
  <Page>
    <PageHeader
      class="home-page-header"
      title="molgenis-viz"
      subtitle="Layout and Data visualization components"
      imageSrc="home-header.jpg"
      height="large"
    />
    <PageSection :verticalPadding="2" aria-labelledby="welcome-title">
      <h2 id="welcome-title">
        Welcome to the <strong>molgenis-viz</strong> library!
      </h2>
      <p>
        The <strong>molgenis-viz</strong> library provides a number of layout
        and visualization components for creating custom molgenis applications.
        The components are not dependent on each other and are minimally styled.
        Therefore, you can pick and choose the elements that suit your needs.
      </p>
      <p>
        There are a number of visualization components included in the library
        and there are more on the way. The visualization components are built
        using D3 v7+ and were designed according to the best data visualization
        practices. All components are customizable and have a number of
        interactive features enabled.
      </p>
    </PageSection>
    <PageSection
      class="bg-gray-050"
      :verticalPadding="2"
      aria-labelledby="components-title"
    >
      <h2 id="components-title">Visualization Components</h2>
      <p>
        This application was built using the library itself to showcase the
        visualization components. Follow a link below to view and interact with
        the component. For the examples, data was sourced from
        <a href="https://ror.org">ROR</a>. All organisations from France,
        Belgium, Germany, and the Netherlands were selected. Metadata was
        collated and compiled into a usable dataset (
        <a href="../tables/#/Organisations"> Organisations table </a>
        ), and used in the examples.
      </p>
      <MessageBox v-if="error || !confirmed" type="error">
        <p><strong>Unable to confirm schema configuration.</strong></p>
        <p>
          Unable to display demo components. It is likely that the schema is not
          yet created. Return to the main home page, sign in, and create a new
          schema using the "ERN_DASHBOARD" template. Make sure the example data
          is loaded.
        </p>
        <code>{{ error }}</code>
      </MessageBox>
      <QuickLinks
        v-else
        id="visualisation-links"
        :data="[
          {
            to: 'bar-chart',
            label: 'Bar Chart',
            image: 'bar-chart-header.jpg',
          },
          {
            to: 'column-chart',
            label: 'Column Chart',
            image: 'column-chart-header.jpg',
          },
          { to: 'datatable', label: 'Data Table', image: 'table-header.jpg' },
          {
            to: 'data-highlights',
            label: 'Data Highlights',
            image: 'highlights-header.jpg',
          },
          {
            to: 'geo-mercator',
            label: 'Geo Mercator',
            image: 'map-header.jpg',
          },
          {
            to: 'grouped-column-chart',
            label: 'Grouped Column Chart',
            image: 'grouped-column-chart-header.jpg',
          },
          { to: 'chart-legend', label: 'Legends', image: 'legend-header.jpg' },
          {
            to: 'pie-chart',
            label: 'Pie Chart',
            image: 'pie-chart-header.jpg',
          },
          {
            to: 'pie-chart-2',
            label: 'Pie Chart 2',
            image: 'pie-chart-header.jpg',
          },
          {
            to: 'scatter-plot',
            label: 'Scatter Plot',
            image: 'scatter-plot-header.jpg',
          },
          {
            to: 'progress-charts',
            label: 'Progress Charts',
            image: 'gauge-chart-header.jpg',
          },
        ]"
        name="to"
        label="label"
        image-src="image"
        cardHeight="auto"
        :should-wrap="true"
      />
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
import QuickLinks from "../components/display/QuickLinks.vue";
import MessageBox from "../components/display/MessageBox.vue";

let loading = ref(true);
let error = ref(null);
let confirmed = ref(false);

async function confirmSchema() {
  const query = gql`
    {
      Organisations {
        name
      }
    }
  `;
  const response = await request("../api/graphql", query);
  confirmed.value = response.Organisations.length > 0;
}

onMounted(() => {
  confirmSchema()
    .catch((err) => {
      if (err.response) {
        error.value = err.response.errors[0].message;
      } else {
        error.value = err;
      }
    })
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
.home-page-header.header-image-background {
  background-position: 0 60%;
}
</style>
