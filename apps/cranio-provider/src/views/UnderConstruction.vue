<script setup lang="ts">
// @ts-expect-error
import { PageSection, MessageBox } from "molgenis-viz";
import { useRoute } from "vue-router";

const route = useRoute();
</script>

<template>
  <PageSection>
    <h2>Under Construction</h2>
    <MessageBox type="error">
      <p>
        The page "{{ route.path }}" is currently under construction. Please
        check back later or return to the
        <router-link :to="{ name: 'home' }">homepage</router-link>.
      </p>
    </MessageBox>
  </PageSection>
</template>
