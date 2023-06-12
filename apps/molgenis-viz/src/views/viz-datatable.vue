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
        <li><router-link :to="{name: 'datatable'}">Datatable</router-link></li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>Datatable</h2>
      <p>The <strong>Datatable</strong> component can be used to display data in tablular format. The table is responsive and can be customised using CSS. All cells, rows, and columns can be selected using values in the data. Click events are also available.</p>
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
        tableId="summaryData"
        :data="data"
        caption="Summary of Observed Penguins by Island"
        :columnOrder="[
          'island',
          'count',
          '% Female',
          '% Male',
          'avg. body bass (g)',
          'avg. flipper length (mm)',
        ]"
        @row-clicked="updateClicked"
      />
      <p>Click a bar in the chart of above to display the row-level data</p>
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
import Breadcrumbs from '@/app-components/breadcrumbs.vue'
import Datatable from '@/components/VizDataTable.vue'

import headerImage from '@/assets/ashley-byrd-unsplash.jpg'

import { fetchData, sortData } from '$shared/js/utils.js'
import { mean, format, rollup, rollups } from 'd3'
const d3 = { mean, format, rollup, rollups }

export default {
  components: {
    Page,
    PageHeader,
    PageSection,
    MessageBox,
    Breadcrumbs,
    Datatable,
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
      const format = d3.format('.2f')
      const summarised = d3
        .rollups(
          data, 
          row => ({
            'count': row.length,
            'avg_body_mass_g' : d3.mean(row, r => r.body_mass_g),
            'avg_flipper_length_mm': d3.mean(row, r => r.flipper_length_mm),
            'males': row.filter(d => d.sex === 'male').length,
            'females': row.filter(d => d.sex === 'female').length
          }),
          row => row.island
        )
        .map(row => new Object({
          island: row[0],
          count: row[1].count,
          'avg. body bass (g)': parseFloat(format(row[1].avg_body_mass_g)),
          'avg. flipper length (mm)': parseFloat(format(row[1].avg_flipper_length_mm)),
          '% Male': parseFloat(format((row[1].males / row[1].count) * 100)),
          '% Female': parseFloat(format((row[1].females / row[1].count) * 100)),
        }))
        
      this.data = sortData(summarised, 'island')
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