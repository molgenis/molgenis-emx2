<script setup lang="ts">
import type { ICollection, IDefinitionListItem } from "~/interfaces/types";
import collectionGql from "~~/gql/collection";

const props = defineProps<{
  id: string;
}>();

const route = useRoute();

const query = moduleToString(collectionGql);

let collection: ICollection = ref();

const { data: collectionData } = await useFetch(
  `/${route.params.schema}/graphql`,
  {
    method: "POST",
    body: {
      query,
      variables: {
        id: props.id,
      },
    },
  }
).catch((e) => console.log(e));

watch(
  collectionData,
  (data) => {
    collection = data?.data?.Collections[0];
  },
  {
    deep: true,
    immediate: true,
  }
);

const items: IDefinitionListItem[] = [];

if (collection?.website) {
  items.push({
    label: "Website",
    type: "LINK",
    content: { label: collection.website, url: collection.website },
  });
}

if (collection?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: collection?.numberOfParticipants,
  });
}

if (collection?.numberOfParticipantsWithSamples) {
  items.push({
    label: "Number of participants with samples",
    content: collection?.numberOfParticipantsWithSamples,
  });
}
</script>

<template>
  <ContentBlockModal
    :title="collection?.name"
    :description="collection?.description"
    v-if="collection"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
