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
        <li><router-link :to="{name: 'bar-chart'}">Bar Chart</router-link></li>
      </Breadcrumbs>
    </PageSection>
    <PageSection class="viz-section">
      <h2>Bar Chart</h2>
      <p>The <strong>BarChart</strong> component is used to display values for categorical data. Groups are plotted along the y-axis and values along the x-axis. If you would like to display values vertically, use the <router-link :to="{name: 'column-chart'}">Column Chart</router-link> component.</p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <BarChart
        v-else
        chartId="penguinsBarChart"
        title="Palmer Penguins by Island"
        description="The following chart displays the number of penguins observed by location (island)."
        :chartData="data"
        xvar="count"
        yvar="island"
        :chartMargins="{left: 110, top: 10, right: 40, bottom: 60}"
        :barPaddingInner="0.25"
        :barPaddingOuter="0.25"
        xAxisLabel="Number of Penguins"
        :enableClicks="true"
        @bar-clicked="updateClicked"
      />
      <p>Click a bar in the chart of above to display the row-level data</p>
      <output class="output">
        {{ clicked }}
      </output>
    </PageSection>
  </Page>
</template>

<script>
import Page from '@/components/layouts/Page.vue'
import PageHeader from '@/components/layouts/PageHeader.vue'
import PageSection from '@/components/layouts/PageSection.vue'
import MessageBox from '@/components/display/MessageBox.vue'
import Breadcrumbs from '@/app-components/breadcrumbs.vue'
import BarChart from '@/components/viz/BarChart.vue'

import headerImage from '@/assets/bulkan-evcimen.jpg'

import { fetchData, sortData } from '@/utils/utils.js'
import { rollups } from 'd3'
const d3 = { rollups }


export default {
  components: {
    Page,
    PageHeader,
    PageSection,
    MessageBox,
    Breadcrumbs,
    BarChart,
  },
  data () {
    return {
      headerImage: headerImage,
      loading: true,
      hasError: false,
      error: null,
      data: [],
      clicked: {},
    }
  },
  methods: {
    updateClicked (data) {
      this.clicked = data
    }
  },
  mounted () {
    Promise.resolve(
      fetchData('/api/v2/rdcomponents_penguins?num=500')
    ).then(response => {
      const data = response.items
      const summarised = d3.rollups(data, row => row.length, row => row.island)
        .map(item => new Object({'island': item[0], 'count': item[1]}))
      this.data = sortData(summarised, 'value')
      this.loading = false
    }).catch(error => {
      const err = error.message
      this.loading = false
      this.hasError = true
      this.error = err
      throw new Error(error)
    })
  }
}
</script>
