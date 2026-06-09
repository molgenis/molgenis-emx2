<template>
  <Page>
    <PageSection class="section-error" aria-labelledby="section-error-title">
      <h2 id="section-error-title">Error!</h2>
      <MessageBox type="error">
        <p>The dashboard is underconstruction</p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script lang="ts" setup>
import {
  Page,
  PageSection,
  MessageBox,
  // @ts-expect-error
} from "molgenis-viz";
</script>
