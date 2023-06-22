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
          <router-link :to="{ name: 'data-highlights' }"
            >Data Highlights</router-link
          >
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
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <DataHighlights :data="summarised" v-else />
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { fetchData, asDataObject } from "@/utils/utils.js";

import Page from "@/components/layouts/Page.vue";
import PageHeader from "@/components/layouts/PageHeader.vue";
import PageSection from "@/components/layouts/PageSection.vue";
import MessageBox from "@/components/display/MessageBox.vue";
import DataHighlights from "@/components/viz/DataHighlights.vue";
import Breadcrumbs from "@/app-components/breadcrumbs.vue";
import headerImage from "@/assets/ray-shrewsberry-unsplash.jpg";

let loading = ref(false);
let hasError = ref(false);
let error = ref(null);
let summarised = ref({});

const query = `{
  Statistics(filter: {component: {name: {equals: "organisations.by.type"}}}) {
    label
    value
    component {
      name
    }
  }
}`;

onMounted(() => {
  Promise.resolve(fetchData(query)).then((response) => {
    const data = asDataObject(response.data.Statistics, "label", "value");
    summarised.value = data;
  });
});
</script>

<style lang="scss">
.data-highlights {
  .data-highlight {
    background-color: $blue-900;
  }
}
</style>
