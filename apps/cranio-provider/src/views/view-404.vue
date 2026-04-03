<template>
  <PageSection>
    <h2>Error 404: Page not found</h2>
    <MessageBox type="error">
      <p>
        The page "{{ route.path }}" cannot be found. To get back on track, click
        a link in the dashboards menu or return to the
        <router-link :to="{ name: 'home' }">homepage</router-link>.
      </p>
    </MessageBox>
  </PageSection>
</template>

<script setup>
import { PageSection, MessageBox } from "molgenis-viz";
import { useRoute } from "vue-router";

const route = useRoute();
</script>
