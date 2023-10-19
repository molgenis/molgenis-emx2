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
      class="bkg-light"
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
      <MessageBox v-if="error || !confirmed">
        <p>
          Unable to confirm schema configuration. Please recreate the schema. If
          an error persists, please open a new issue on GitHub. {{ error }}
        </p>
      </MessageBox>
      <div class="link-container" v-else>
        <LinkCard imageSrc="bar-chart-header.jpg">
          <router-link :to="{ name: 'bar-chart' }"> Bar Chart </router-link>
        </LinkCard>
        <LinkCard imageSrc="column-chart-header.jpg">
          <router-link :to="{ name: 'column-chart' }">
            Column Chart
          </router-link>
        </LinkCard>
        <LinkCard imageSrc="table-header.jpg">
          <router-link :to="{ name: 'datatable' }"> Data Table </router-link>
        </LinkCard>
        <LinkCard imageSrc="highlights-header.jpg">
          <router-link :to="{ name: 'data-highlights' }">
            Data Highlights
          </router-link>
        </LinkCard>
        <LinkCard imageSrc="map-header.jpg">
          <router-link :to="{ name: 'geo-mercator' }">
            GeoMercator
          </router-link>
        </LinkCard>
        <LinkCard imageSrc="grouped-column-chart-header.jpg">
          <router-link :to="{ name: 'grouped-column-chart' }">
            Grouped Column Chart
          </router-link>
        </LinkCard>
        <LinkCard imageSrc="legend-header.jpg">
          <router-link :to="{ name: 'chart-legend' }"> Legends </router-link>
        </LinkCard>
        <LinkCard imageSrc="pie-chart-header.jpg">
          <router-link :to="{ name: 'pie-chart' }"> Pie Chart </router-link>
        </LinkCard>
        <LinkCard imageSrc="pie-chart-header.jpg">
          <router-link :to="{ name: 'pie-chart-2' }"> Pie Chart2 </router-link>
        </LinkCard>
        <LinkCard imageSrc="scatter-plot-header.jpg">
          <router-link :to="{ name: 'scatter-plot' }">
            Scatter Plot
          </router-link>
        </LinkCard>
        <LinkCard imageSrc="gauge-chart-header.jpg">
          <router-link :to="{ name: 'progress-charts' }">
            Progress Charts
          </router-link>
        </LinkCard>
      </div>
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
import LinkCard from "../components/display/LinkCard.vue";
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
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
.home-page-header.header-image-background {
  background-position: 0 60%;
}
.link-container {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 2em;

  .link-card {
    height: 6em;
    border-radius: 12px;
    background-color: transparent;
    flex-grow: 1;

    a {
      color: $green-050;
    }
    .card-background-filter {
      border-radius: 12px;
      background-color: $blue-700;
      opacity: 0.5;
    }
  }
}
</style>
