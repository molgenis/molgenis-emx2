<script setup lang="ts">
import ContentBlockModal from "./content/ContentBlockModal.vue";
import variableQuery from "~~/gql/variable";

const query = moduleToString(variableQuery);

const props = defineProps<{
  name: string;
}>();

const config = useRuntimeConfig();
const route = useRoute();

const { data, pending, error } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: query,
      variables: {
        name: props.name,
      },
    },
  }
).catch((e) => console.log(e));
</script>

<template>
  <ContentBlockModal
    :title="data.data?.Variables[0].name"
    :description="data.data?.Variables[0].description"
    sub-title="Variable"
  >
    <!-- <DefinitionList :items="items" :small="true" /> -->
  </ContentBlockModal>
</template>
