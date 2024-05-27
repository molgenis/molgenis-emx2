<script setup lang="ts">
type Resp<T> = {
  data: Record<string, T[]>;
};

interface Schema {
  id: string;
  label: string;
  description: string;
}

const { data } = await useFetch<Resp<Schema>>("/graphql", {
  key: "databases",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const databases = computed(
  () =>
    data.value?.data?._schemas.sort((a, b) => a.label.localeCompare(b.label)) ??
    []
);

const store = useMetaStore();
const { metaData } = storeToRefs(store);
</script>
<template>
  <Container>
    <DisplayList class="text-white" title="Databases" :columnCount="1">
      <DisplayListItem v-for="database in databases">
        <NuxtLink :to="`/${database.id}`">{{ database.label }}</NuxtLink>
      </DisplayListItem>
    </DisplayList>
  </Container>
  {{ metaData }}
</template>
