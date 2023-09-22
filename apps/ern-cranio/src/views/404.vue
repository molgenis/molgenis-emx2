<template>
  <Page>
    <PageSection :verticalPadding="2">
      <MessageBox type="warning">
        <p>
          The page you searched for does not exist.
          <router-link :to="{ name: 'home' }">Return to home.</router-link>
        </p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageSection, MessageBox } from "molgenis-viz";
</script>
