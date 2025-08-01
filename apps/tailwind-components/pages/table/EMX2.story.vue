<script setup lang="ts">
import { useFetch } from "#app";
import { fetchMetadata } from "#imports";
import { computed, ref, watch } from "vue";
import type { ITableSettings, Resp, Schema } from "../../types/types";

const tableSettings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: { column: "", direction: "ASC" },
  search: "",
});

const isEditable = ref(false);

const { data } = await useFetch<Resp<Schema>>("/graphql", {
  key: "databases",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const databases = computed(
  () =>
    data.value?.data?._schemas.sort((a: any, b: any) =>
      a.label.localeCompare(b.label)
    ) ?? []
);

const schemaId = ref(
  databases.value.find(
    (database: any) =>
      database.label === "pet store" || database.id === "catalogue-demo"
  )?.id || ""
);

const metadata = ref(await fetchMetadata(schemaId.value));

watch(
  schemaId,
  async () => {
    if (schemaId.value) {
      metadata.value = await fetchMetadata(schemaId.value);
      if (metadata.value) {
        tableId.value = metadata.value.tables[0].id;
      }
    }
  },
  { immediate: true }
);

const tableId = ref(
  schemaId.value === "pet store"
    ? "Pet"
    : schemaId.value === "catalogue-demo"
    ? "Resources"
    : ""
);

const schemaOptions = computed(() =>
  databases.value.map((schema: any) => schema.id)
);

if (metadata.value) {
  tableId.value = metadata.value.tables[0].id;
}

const tableOptions = computed(() => {
  if (metadata.value) {
    return metadata.value.tables.map((table) => table.id);
  } else {
    return [];
  }
});
</script>

<template>
  <div class="mt-4 mb-16">
    <h3 class="text-heading-lg">Params</h3>
    <div class="m-2">
      <label for="schema-id-input">schema id: </label>
      <select id="schema-id-input" v-model="schemaId">
        <option v-for="option in schemaOptions" :value="option">
          {{ option }}
        </option>
      </select>
    </div>
    <div class="m-2">
      <label for="table-id-select">table id: </label>
      <select id="table-id-select" v-model="tableId">
        <option v-for="option in tableOptions" :value="option">
          {{ option }}
        </option>
      </select>
    </div>
    <div class="m-2">
      <label for="table-id-select">Edit mode: </label>
      <InputBoolean
        v-model="isEditable"
        id="edit-mode-input"
        :showClearButton="false"
      />
    </div>
  </div>

  <div>
    <div><span class="text-title">tableId:</span> {{ tableId }}</div>
    <div class="mb-4">
      <span class="text-title">schemaId:</span> {{ schemaId }}
    </div>

    <div>
      <TableEMX2
        v-if="tableId && schemaId"
        :key="`${schemaId}-${tableId}`"
        :schema-id="schemaId"
        :table-id="tableId"
        v-model:settings="tableSettings"
        :is-editable="isEditable"
      />
    </div>
  </div>
</template>
