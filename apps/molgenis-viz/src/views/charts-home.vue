<template>
  <Page>
    <PageHeader
      title="molgenis-viz"
      subtitle="Chart library"
      height="medium"
      class="bg-blue-050"
    />
    <PageSection :verticalPadding="2" width="large">
      <h2>Available charts</h2>
      <MessageBox type="warning">
        <p>
          No charts founds. Charts created using the
          <router-link
            :to="{ name: 'charts-builder' }"
            style="text-decoration: underline"
            >Chart Builder</router-link
          >
          will appear here.
        </p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
</script>
