<script setup lang="ts">
import { useRoute, useRuntimeConfig, useFetch, createError } from "#app";
import { logError } from "#imports";
import { computed } from "vue";
import { RESERVED_ROUTES } from "../../utils/constants";
import CatalogueLandingView from "../../components/CatalogueLandingView.vue";
import CollectionDetailView from "../../components/CollectionDetailView.vue";

const route = useRoute();
const config = useRuntimeConfig();
const schema = config.public.schema as string;

const resourceId = route.params.resourceId as string;

if (RESERVED_ROUTES.includes(resourceId)) {
  throw createError({
    statusCode: 404,
    statusMessage: `Resource "${resourceId}" is a reserved route.`,
  });
}

const typeCheckQuery = `query TypeCheck($id: String) {
  Resources(filter: { id: { equals: [$id] } }) {
    id
    name
    type {
      name
    }
  }
}`;

const { data, error } = await useFetch(`/${schema}/graphql`, {
  method: "POST",
  key: `type-check-${resourceId}`,
  body: {
    query: typeCheckQuery,
    variables: { id: resourceId },
  },
});

if (error.value) {
  const contextMsg = "Error on resource type check";
  if (error.value.data) {
    logError(error.value.data, contextMsg);
  }
  throw new Error(contextMsg);
}

const resource = computed(() => data.value?.data?.Resources?.[0]);

if (!resource.value) {
  throw createError({
    statusCode: 404,
    statusMessage: `Resource "${resourceId}" not found.`,
  });
}

const isCatalogue = computed(() => {
  return resource.value?.type?.some(
    (t: { name?: string }) => t.name === "Catalogue"
  );
});
</script>

<template>
  <CatalogueLandingView v-if="isCatalogue" :resource-id="resourceId" />
  <CollectionDetailView v-else :resource-id="resourceId" />
</template>
