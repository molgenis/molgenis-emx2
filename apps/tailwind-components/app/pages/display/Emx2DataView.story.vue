<template>
  <Story title="Emx2DataView" :spec="spec">
    <DemoDataControls
      v-model:schemaId="schemaId"
      v-model:tableId="tableId"
      v-model:metadata="metadata"
    />

    <div class="flex gap-4 mb-4 flex-wrap">
      <button
        v-for="mode in viewModes"
        :key="mode.id"
        @click="viewMode = mode.id"
        :class="[
          'px-4 py-2 rounded font-semibold',
          viewMode === mode.id ? 'bg-blue-500 text-white' : 'bg-gray-200',
        ]"
      >
        {{ mode.label }}
      </button>
    </div>

    <div class="flex gap-4 mb-4">
      <button
        v-for="mode in layouts"
        :key="mode"
        @click="layout = mode"
        :class="[
          'px-4 py-2 rounded font-semibold text-sm',
          layout === mode ? 'bg-gray-700 text-white' : 'bg-gray-100',
        ]"
      >
        {{ mode }}
      </button>
    </div>

    <div v-if="viewMode === 'full'" class="mb-8">
      <h3 class="text-lg font-semibold mb-2">Full Page (with header)</h3>
      <DetailPageLayout>
        <template #header>
          <PageHeader
            :title="tableId"
            :description="`View all ${tableId} records`"
          >
            <template #prefix>
              <BreadCrumbs
                :crumbs="[
                  { label: schemaId, url: '#' },
                  { label: tableId, url: '#' },
                ]"
              />
            </template>
          </PageHeader>
        </template>
        <template #main>
          <Emx2DataView
            :key="`full-${schemaId}-${tableId}`"
            :schema-id="schemaId"
            :table-id="tableId"
            :config="{
              layout: layout,
              showFilters: true,
              filterPosition: 'sidebar',
              showSearch: true,
              pageSize: 10,
            }"
          />
        </template>
      </DetailPageLayout>
    </div>

    <div v-if="viewMode === 'compact'" class="mb-8">
      <h3 class="text-lg font-semibold mb-2">Compact (no header, with filters)</h3>
      <Emx2DataView
        :key="`compact-${schemaId}-${tableId}`"
        :schema-id="schemaId"
        :table-id="tableId"
        :config="{
          layout: layout,
          showFilters: true,
          filterPosition: 'sidebar',
          showSearch: true,
          pageSize: 10,
        }"
      />
    </div>

    <div v-if="viewMode === 'vanilla'" class="mb-8">
      <h3 class="text-lg font-semibold mb-2">Vanilla (no header, no filters)</h3>
      <Emx2DataView
        :key="`vanilla-${schemaId}-${tableId}`"
        :schema-id="schemaId"
        :table-id="tableId"
        :config="{
          layout: layout,
          showFilters: false,
          showSearch: true,
          pageSize: 10,
        }"
      />
    </div>
  </Story>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import DemoDataControls from "../../DemoDataControls.vue";
import Emx2DataView from "../../components/display/Emx2DataView.vue";
import DetailPageLayout from "../../components/layout/DetailPageLayout.vue";
import PageHeader from "../../components/PageHeader.vue";
import BreadCrumbs from "../../components/BreadCrumbs.vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";

const route = useRoute();
const router = useRouter();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const layouts = ["list", "table", "cards"] as const;
const layout = ref<(typeof layouts)[number]>("table");

const viewModes = [
  { id: "full", label: "Full Page" },
  { id: "compact", label: "Compact (no header)" },
  { id: "vanilla", label: "Vanilla (no filters)" },
] as const;
const viewMode = ref<(typeof viewModes)[number]["id"]>("full");

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});

const spec = `
Unified data view with three usage modes.

## View Modes
| Mode | Header | Filters | Use Case |
|------|--------|---------|----------|
| Full Page | Yes | Yes | Catalogue-style pages |
| Compact | No | Yes | Embedded with filters |
| Vanilla | No | No | Simple data display |

## Props (via config)
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| layout | 'list' \\| 'table' \\| 'cards' | table | Display mode |
| showFilters | boolean | false | Show filter sidebar |
| filterPosition | 'sidebar' \\| 'topbar' | sidebar | Filter placement |
| filterableColumns | string[] | - | Limit filterable columns |
| visibleColumns | string[] | - | Columns shown in table |
| showSearch | boolean | true | Show search input |
| pageSize | number | 10 | Items per page |
| rowLabel | string | - | Template like "\${name}" |
| urlSync | boolean | true | Sync filters to URL |

## Responsive Behavior
- Desktop (xl+): Filter sidebar visible
- Mobile (<xl): "Filters" button → modal

## Slots
- \`#header\` - Optional page header
- \`#default\` - Custom list item (props: row, label)
- \`#card\` - Custom card content (props: row, label)

## Test Checklist
- [ ] Full Page mode shows header + sidebar filters
- [ ] Compact mode shows sidebar filters, no header
- [ ] Vanilla mode shows data only, no filters
- [ ] Mobile: "Filters" button appears in compact mode
- [ ] Switch layouts - filters persist
- [ ] URL sync works across all modes
`;
</script>
