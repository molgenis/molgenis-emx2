<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Pie Chart"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li><router-link :to="{name: 'pie-chart'}">Pie Chart</router-link></li>
      </Breadcrumbs>
    </PageSection>
    <PageSection class="viz-section">
      <h2>PieChart Component</h2>
      <p>The <strong>PieChart</strong> component is used to descriptives for categorical data. Input data must be an object with one or more key-value pairs. It is recommended to supply no more than 7 categories and to combine smaller groups into an "other" category. If you need to display more groups, it is strongly recommended to use the <strong>BarChart</strong> or <strong>ColumnChart</strong> components. Alternatively, the <strong>DataTable</strong> component is much better.</p>
      <p>It is also possible to enable click events to enhance interactivity with other visualisation components. See the example below.</p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <PieChart
        v-else
        chartId="sexByPenguin"
        title="Summary of species"
        :description="`In total, ${total} penguins were observed across all stations. The following chart shows the breakdown of observed penguins by species.`"
        :chartData="data"
        :enableClicks="true"
        :chartHeight="200"
        @slice-clicked="updateSelection"

      />
      <h3>Selected Item</h3>
      <p>Click a slice in the chart of above to display the row-level data</p>
      <output class="output">
        {{ clicked }}
      </output>
    </PageSection>
  </Page>
</template>

<script>
import Page from '@/components/Page.vue'
import PageHeader from '@/components/PageHeader.vue'
import PageSection from '@/components/PageSection.vue'
import MessageBox from '@/components/MessageBox.vue'
import PieChart from '@/components/VizPieChart.vue'
import Breadcrumbs from '@/app-components/breadcrumbs.vue'

import { fetchData } from '$shared/js/utils.js' 
import { rollups, sum, format } from 'd3'
const d3 = { rollups, sum, format }

import headerImage from '@/assets/sheri-silver-unsplash.jpg'

export default {
  components: {
    Page,
    PageHeader,
    PageSection,
    MessageBox,
    PieChart,
    Breadcrumbs
  },
  data () {
    return {
      headerImage: headerImage,
      loading: true,
      hasError: false,
      error: null,
      data: [],
      total: 0,
      clicked: {},
    }
  },
  methods: {
    updateSelection (value) {
      this.clicked = value
    }
  },
  mounted () {
    Promise.resolve(
      fetchData('/api/v2/rdcomponents_penguins?num=500')
    ).then(response => {
      const data = response.items
      const format = d3.format('.2f')
      const grouped = d3.rollups(data, row => row.length, row => row.species)
      const summarised = {} 
      grouped.map(array => {
        if (typeof array[0] === 'undefined') {
          summarised['unknown'] = array[1]
        } else {
          summarised[array[0]] = format((array[1] / data.length) * 100)
        }
      })
      this.data = summarised
      this.total = d3.sum(grouped, item => item[1])
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