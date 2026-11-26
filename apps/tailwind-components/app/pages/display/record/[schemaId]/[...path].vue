<script setup lang="ts">
import { computed } from "vue";
import { useAsyncData } from "#app";
import { useRoute } from "vue-router";
import fetchGraphql from "../../../../composables/fetchGraphql";
import fetchMetadata from "../../../../composables/fetchMetadata";
import BreadCrumbs from "../../../../components/BreadCrumbs.vue";
import DisplayRecord from "../../../../components/display/Record.vue";
import Button from "../../../../components/Button.vue";
import Message from "../../../../components/Message.vue";
import { deriveRecordQuery } from "../../../../utils/deriveRecordQuery";
import { resolveRecordPath } from "../../../../utils/recordPath";
import { enhanceMetadata } from "../../../../utils/enhanceMetadata";
import { tableConfigs } from "../../../../demoTableConfigs";

const route = useRoute();
const schemaId = String(route.params.schemaId);
const basePath = `/display/record/${schemaId}`;
const segments = [route.params.path ?? []].flat();

const { data, error } = await useAsyncData(
  `record-${schemaId}-${segments.join("/")}`,
  async () => {
    const schema = enhanceMetadata(await fetchMetadata(schemaId), tableConfigs);
    const path = resolveRecordPath(schema, tableConfigs, basePath, segments);
    const table = schema.tables.find(
      (candidate) => candidate.id === path.tableId
    );
    if (!table) {
      throw new Error(`Table ${path.tableId} not in schema ${schemaId}`);
    }
    const response = await fetchGraphql(
      schemaId,
      deriveRecordQuery(schema, path.tableId),
      { filter: path.filter }
    );
    return {
      tableId: path.tableId,
      schema,
      columns: table.columns,
      row: response[path.tableId][0] ?? null,
      crumbs: path.crumbs,
    };
  }
);

const intro = computed(() => {
  const row = data.value?.row;
  if (data.value?.tableId !== "Collections" || !row) {
    return null;
  }
  const { website, contactEmail } = row;
  return {
    website: typeof website === "string" ? website : null,
    contactEmail: typeof contactEmail === "string" ? contactEmail : null,
  };
});
</script>

<template>
  <Message v-if="error" id="record-error" invalid>
    Could not load {{ segments.join("/") }}.
  </Message>
  <Message v-else-if="!data?.row" id="record-missing">
    No record at {{ segments.join("/") }}.
  </Message>
  <DisplayRecord
    v-else
    :columns="data.columns"
    :row="data.row"
    :config="tableConfigs[data.tableId]"
    :schema="data.schema"
    :recordPath="data.crumbs[data.crumbs.length - 1]?.url"
  >
    <template #header>
      <BreadCrumbs :crumbs="data.crumbs" align="left" class="mb-7.5" />
    </template>
    <template v-if="intro" #intro>
      <div
        data-testid="record-intro"
        class="bg-content text-title-contrast shadow-primary rounded-base p-5 flex flex-wrap items-center justify-between gap-5"
      >
        <a
          v-if="intro.website"
          :href="intro.website"
          class="text-link underline break-all"
          >{{ intro.website }}</a
        >
        <Button
          v-if="intro.contactEmail"
          type="primary"
          size="medium"
          label="Contact"
          :href="`mailto:${intro.contactEmail}`"
        />
      </div>
    </template>
  </DisplayRecord>
</template>
