<script setup lang="ts">
import { useAsyncData } from "#app";
import { queryCollection } from "#imports";

const { data: page } = await useAsyncData("get-started", () =>
  queryCollection("docs").path("/get-started").first()
);
</script>

<template>
  <div class="px-12 py-8 max-w-3xl">
    <ContentRenderer
      v-if="page"
      :value="page"
      class="prose text-record-default"
    />
    <p v-else class="text-record-subtle">Content not found.</p>
  </div>
</template>
