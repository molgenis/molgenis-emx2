<template>
  <Story title="Emx2DataView" :spec="spec">
    <DemoDataControls
      v-model:schemaId="schemaId"
      v-model:tableId="tableId"
      v-model:metadata="metadata"
    />

    <div class="flex gap-4 mb-4">
      <button
        v-for="mode in layouts"
        :key="mode"
        @click="layout = mode"
        :class="[
          'px-4 py-2 rounded font-semibold',
          layout === mode ? 'bg-blue-500 text-white' : 'bg-gray-200',
        ]"
      >
        {{ mode }}
      </button>
    </div>

    <Emx2DataView
      :key="`${schemaId}-${tableId}`"
      :schema-id="schemaId"
      :table-id="tableId"
      :layout="layout"
      :show-filters="true"
      filter-position="sidebar"
      :show-search="true"
      :paging-limit="10"
    />
  </Story>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import DemoDataControls from "../../DemoDataControls.vue";
import Emx2DataView from "../../components/display/Emx2DataView.vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";

const route = useRoute();
const router = useRouter();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const layouts = ["list", "table", "cards"] as const;
const layout = ref<(typeof layouts)[number]>("list");

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});

const spec = `
Unified data view combining fetching, layout options, and filter support.

## Features
- Single component replaces FilterSidebar + ListView/TableEMX2
- Three layout modes: list, table, cards
- Integrated filter sidebar with debounced updates (300ms)
- Auto-computed filterable columns from metadata
- Search input with pagination

## Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| layout | 'list' \\| 'table' \\| 'cards' | list | Display mode |
| showFilters | boolean | false | Show filter sidebar |
| filterPosition | 'sidebar' \\| 'topbar' | sidebar | Filter placement |
| filterableColumns | string[] | - | Limit which columns are filterable |
| visibleColumns | string[] | - | Columns shown in table layout |
| showSearch | boolean | true | Show search input |
| pagingLimit | number | 10 | Items per page |
| rowLabel | string | - | Template like "\${name}" |
| displayOptions | Record<colId, {href?, onClick?}> | - | Column link options |
| urlSync | boolean | true | Sync filters to URL |

## Slots
- \`#default\` - Custom list item (props: row, label)
- \`#card\` - Custom card content (props: row, label)

## Test Checklist
- [ ] Select schema/table - component updates
- [ ] All filterable columns appear in sidebar
- [ ] Expand STRING filter - type text - results filter, URL updates
- [ ] Expand REF filter - select value - results update, URL updates
- [ ] Set INT/DECIMAL min/max - between filter applies, URL updates
- [ ] Clear individual filter - results refresh, URL clears param
- [ ] Switch between list/table/cards - same filters apply
- [ ] Search works in all layout modes
- [ ] Pagination updates correctly
- [ ] Browser back/forward restores filter state
`;
</script>
