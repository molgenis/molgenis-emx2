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

    <div class="flex gap-4 mb-4 items-center">
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
      <label class="flex items-center gap-2 ml-4">
        <input type="checkbox" v-model="isEditable" class="w-4 h-4" />
        <span class="text-sm">isEditable</span>
      </label>
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
            :key="`full-${schemaId}-${tableId}-${layout}`"
            :schema-id="schemaId"
            :table-id="tableId"
            :is-editable="isEditable"
            :config="{
              layout: configLayout,
              showLayoutToggle: true,
              showFilters: true,
              filterPosition: 'sidebar',
              showSearch: true,
              pageSize: 10,
            }"
          >
            <template v-if="layout === 'custom'" #default="{ row, columns }">
              <div class="flex flex-col gap-1">
                <h4 class="font-bold text-lg">
                  {{ columns[0] ? row[columns[0].id] : "" }}
                </h4>
                <div
                  v-for="col in columns.slice(1)"
                  :key="col.id"
                  class="text-sm text-gray-600"
                >
                  {{ col.label }}: {{ row[col.id] }}
                </div>
              </div>
            </template>
          </Emx2DataView>
        </template>
      </DetailPageLayout>
    </div>

    <div v-if="viewMode === 'compact'" class="mb-8">
      <h3 class="text-lg font-semibold mb-2">
        Compact (no header, with filters)
      </h3>
      <Emx2DataView
        :key="`compact-${schemaId}-${tableId}-${layout}`"
        :schema-id="schemaId"
        :table-id="tableId"
        :is-editable="isEditable"
        :config="{
          layout: configLayout,
          showLayoutToggle: true,
          showFilters: true,
          filterPosition: 'sidebar',
          showSearch: true,
          pageSize: 10,
        }"
      >
        <template v-if="layout === 'custom'" #default="{ row, columns }">
          <div class="flex flex-col gap-1">
            <h4 class="font-bold text-lg">
              {{ columns[0] ? row[columns[0].id] : "" }}
            </h4>
            <div
              v-for="col in columns.slice(1)"
              :key="col.id"
              class="text-sm text-gray-600"
            >
              {{ col.label }}: {{ row[col.id] }}
            </div>
          </div>
        </template>
      </Emx2DataView>
    </div>

    <div v-if="viewMode === 'vanilla'" class="mb-8">
      <h3 class="text-lg font-semibold mb-2">
        Vanilla (no header, no filters)
      </h3>
      <Emx2DataView
        :key="`vanilla-${schemaId}-${tableId}-${layout}`"
        :schema-id="schemaId"
        :table-id="tableId"
        :is-editable="isEditable"
        :config="{
          layout: configLayout,
          showFilters: false,
          showSearch: true,
          pageSize: 10,
        }"
      >
        <template v-if="layout === 'custom'" #default="{ row, columns }">
          <div class="flex flex-col gap-1">
            <h4 class="font-bold text-lg">
              {{ columns[0] ? row[columns[0].id] : "" }}
            </h4>
            <div
              v-for="col in columns.slice(1)"
              :key="col.id"
              class="text-sm text-gray-600"
            >
              {{ col.label }}: {{ row[col.id] }}
            </div>
          </div>
        </template>
      </Emx2DataView>
    </div>
  </Story>
</template>

<script setup lang="ts">
import { ref, watch, computed } from "vue";
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
const layouts = ["list", "table", "cards", "custom"] as const;
const layout = ref<(typeof layouts)[number]>("table");
const isEditable = ref(false);
const configLayout = computed(() =>
  layout.value === "custom" ? "cards" : layout.value
);

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
Unified data view with three usage modes and slot customization demo.

## View Modes
| Mode | Header | Filters | Use Case |
|------|--------|---------|----------|
| Full Page | Yes | Yes | Catalogue-style pages |
| Compact | No | Yes | Embedded with filters |
| Vanilla | No | No | Simple data display |

## Layouts
- list, table, cards: built-in layouts
- custom: uses cards layout with #default slot for custom rendering

## Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| schemaId | string | required | Schema identifier |
| tableId | string | required | Table identifier |
| isEditable | boolean | false | Show add/edit/delete buttons |
| config | IDisplayConfig | {} | Display configuration |

## Config Options (IDisplayConfig)
| Option | Type | Default | Description |
|------|------|---------|-------------|
| layout | 'list' \\| 'table' \\| 'cards' | table | Default display mode |
| showLayoutToggle | boolean | false | Show table/cards toggle in toolbar |
| showFilters | boolean | false | Enable filter sidebar |
| filterPosition | 'sidebar' \\| 'topbar' | sidebar | Filter placement |
| filterableColumns | string[] | - | Limit filterable columns |
| visibleColumns | string[] | - | Columns shown in table |
| showSearch | boolean | true | Show search input |
| pageSize | number | 10 | Items per page |
| rowLabel | string | - | Template like "\${name}" |
| urlSync | boolean | true | Sync filters to URL |

## Toolbar (desktop)
- Add button (when isEditable)
- Toggle table/cards (when showLayoutToggle)
- Columns button
- Show/Hide Filters button (when showFilters + sidebar)

## Responsive Behavior
- Desktop (xl+): Filter sidebar visible
- Mobile (<xl): "Filters" button → modal

## Slots
- \`#header\` - Optional page header
- \`#default\` - Custom item content (props: row, label, columns)

## Row Actions (when isEditable=true)
- Edit/delete buttons in first column (sticky)
- Edit button opens EditModal
- Delete button opens DeleteModal
- Mobile: actions in RecordCard

## Test Checklist
- [ ] Full Page mode shows header + sidebar filters
- [ ] Compact mode shows sidebar filters, no header
- [ ] Vanilla mode shows data only, no filters
- [ ] Mobile: "Filters" button appears in compact mode
- [ ] Switch layouts - filters persist
- [ ] URL sync works across all modes
- [ ] isEditable: Add button appears in toolbar
- [ ] isEditable: Edit/delete buttons appear per row
- [ ] Edit button opens modal, saves, refreshes
- [ ] Delete button opens modal, deletes, refreshes
`;
</script>
