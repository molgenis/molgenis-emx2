<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Layout and Data Visualisation components"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="2" aria-labelledby="welcome-title">
      <h2 id="welcome-title">
        Welcome to the <strong>molgenis-viz</strong> library!
      </h2>
      <p>
        The <strong>molgenis-viz</strong> library contains D3-based
        visualisation components and other layout components for use in MOLGENIS
        applications. This allows us to standarise our custom applications while
        allowing for project-specific requirements (e.g., branding, logos,
        etc.).
      </p>
      <p>
        The <strong>molgenis-viz</strong> demo application was built using the
        library itself to showcase the visualisation components. There are few
        visualisation components available and more on the way. Follow the links
        below to view a demonstration of the components. The Data used in the
        examples visualisations was sourced from the
        <a href="https://ror.org">ROR Database</a> and is stored in the
        Dashboard schema.
      </p>
    </PageSection>
    <PageSection
      class="bkg-light"
      :verticalPadding="2"
      aria-labelledby="components-title"
    >
      <h2 id="components-title">Visualisation Components</h2>
      <div class="link-container">
        <LinkCard :imageSrc="BarChartImage">
          <router-link :to="{ name: 'bar-chart' }">Bar Chart</router-link>
        </LinkCard>
        <LinkCard :imageSrc="LegendImage">
          <router-link :to="{ name: 'chart-legend' }">Legends</router-link>
        </LinkCard>
        <LinkCard :imageSrc="ColumnChartImage">
          <router-link :to="{ name: 'column-chart' }">Column Chart</router-link>
        </LinkCard>
        <LinkCard :imageSrc="DatatableImage">
          <router-link :to="{ name: 'datatable' }">Data Table</router-link>
        </LinkCard>
        <LinkCard :imageSrc="GroupedColumnChartImage">
          <router-link :to="{ name: 'grouped-column-chart' }">
            Grouped Column Chart
          </router-link>
        </LinkCard>
        <LinkCard :imageSrc="HighlightsImage">
          <router-link :to="{ name: 'data-highlights' }">
            Data Highlights
          </router-link>
        </LinkCard>
        <LinkCard :imageSrc="MapImage">
          <router-link :to="{ name: 'geo-mercator' }">GeoMercator</router-link>
        </LinkCard>
        <LinkCard :imageSrc="PieChartImage">
          <router-link :to="{ name: 'pie-chart' }">Pie Chart</router-link>
        </LinkCard>
        <LinkCard :imageSrc="ScatterPlotImage">
          <router-link :to="{ name: 'scatter-plot' }">Scatter Plot</router-link>
        </LinkCard>
        <LinkCard :imageSrc="GaugeChartImage">
          <router-link :to="{ name: 'progress-charts' }">Progress Charts</router-link>
        </LinkCard>
      </div>
    </PageSection>
  </Page>
</template>

<script setup>
import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import LinkCard from "../components/display/LinkCard.vue";

import headerImage from "../assets/header-image.jpg";
import BarChartImage from "../assets/bar-chart-header.jpg";
import ColumnChartImage from "../assets/column-chart-header.jpg";
import DatatableImage from "../assets/table-header.jpg";
import GaugeChartImage from "../assets/gauge-chart-header.jpg";
import GroupedColumnChartImage from "../assets/grouped-column-chart-header.jpg";
import HighlightsImage from "../assets/highlights-header.jpg";
import LegendImage from "../assets/legend-header.jpg";
import MapImage from "../assets/map-header.jpg";
import PieChartImage from "../assets/pie-chart-header.jpg";
import ScatterPlotImage from "../assets/scatter-plot-header.jpg";
</script>

<style lang="scss">
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
