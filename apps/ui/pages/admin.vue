<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management"> </ContentBlock>
  </Container>
</template>

<script lang="ts">
definePageMeta({
  middleware: "admin-only",
});
</script>
