<script lang="ts" setup>
import { definePageMeta } from "#imports";
import { ref } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import type {
  cellPayload,
  ITableSettings,
} from "../../../../tailwind-components/types/types.ts";
import useAdminSettings from "../../composables/useAdminSettings.ts";
import type {
  IColumn,
  ITableMetaData,
  TableRow,
} from "../../../../metadata-utils/src/types.ts";

definePageMeta({
  middleware: "admin-only",
});

const DEFAULT_TABLE_SETTINGS: ITableSettings = {
  page: 1,
  pageSize: 50,
  orderby: {
    column: "email",
    direction: "ASC",
  },
};

const tableFeatures = {
  showSearch: true,
  showStickyHeader: true,
};

const tablePermissions = {
  canInsert: true,
  canUpdate: true,
  canDelete: true,
  canModifySelection: true,
  isRowLevel: false,
};

const COLUMNS: IColumn[] = [
  { label: "Schema", id: "schemaId", columnType: "STRING" },
  { label: "Role Name", id: "roleName", columnType: "STRING" },
  { label: "Tables", id: "tables", columnType: "STRING" },
  { label: "Users", id: "users", columnType: "STRING" },
];

const tableMetadata: ITableMetaData = {
  id: "roles",
  schemaId: "roles",
  name: "roles",
  label: "Roles",
  tableType: "DATA",
  columns: COLUMNS,
};

const rows = ref<TableRow[]>([]);
const selectedRows = ref<Map<string, any>>(new Map());
const tableSettings = ref<ITableSettings>(DEFAULT_TABLE_SETTINGS);
const showCellDetailSideModal = ref(false);
const cellDetailPayload = ref<cellPayload>();

const adminSettings = await useAdminSettings();

function handleUpdateSettings(newSettings: ITableSettings) {
  tableSettings.value = newSettings;
}
function handleCellClicked(payload: cellPayload) {
  cellDetailPayload.value = payload;
  showCellDetailSideModal.value = true;
}

function handleToggleRowSelection(row: TableRow) {
  if (selectedRows.value.has(row._rowIdString)) {
    selectedRows.value.delete(row._rowIdString);
  } else {
    selectedRows.value.set(row._rowIdString, row);
  }
}

function handleRowAction(payload: { action: string }) {
  if ("action" in payload) {
    const action = payload.action;
    const singleRowSelected =
      selectedRows.value.size === 1
        ? rows.value.find((row) => selectedRows.value.has(row._rowIdString))
        : null;
    switch (action) {
      case "delete-selection":
        // showDeleteUserConfirmation();
        break;
      case "edit-selection":
        if (singleRowSelected) {
          //   openEditUserModal(singleRowSelected);
        }
        break;
      case "select-all-on-page":
        rows.value.forEach((row) => {
          selectedRows.value.set(row._rowIdString, row);
        });
        break;
      case "select-none":
        selectedRows.value.clear();
        break;
    }
  }
}
</script>

<template>
  <Container class="flex flex-col items-center">
    <ContentBlock
      class="w-full mt-3"
      title="Role management"
      :description="`${rows.length} roles found`"
    >
      <TableInteractive
        :features="tableFeatures"
        :permissions="tablePermissions"
        :rows="rows"
        :rowCount="rows.length"
        :selectedRows="selectedRows"
        :settings="tableSettings"
        :tableMetadata="tableMetadata"
        searchPlaceholder="Search roles"
        @update:settings="handleUpdateSettings"
        @cellClicked="handleCellClicked"
        @rowAction="handleRowAction"
        @toggleRowSelection="handleToggleRowSelection"
      >
        <template #row-action="{ row }">
          <Button
            :id="`edit-role-${row.email}`"
            iconOnly
            type="inline"
            icon="edit"
            :label="`Edit role: ${row.email}`"
            @click=""
          />
          <Button
            :id="`delete-role-${row.email}`"
            iconOnly
            type="inline"
            icon="trash"
            :label="`Delete role: ${row.email}`"
            @click=""
          />
          <Button
            v-if="Array.isArray(row.tokens) && row.tokens.length"
            :id="`manage-tokens-${row.email}`"
            type="inline"
            size="tiny"
            @click=""
          >
            {{ row.tokens?.length }} tokens
          </Button>
        </template>

        <template #buttons>
          <Button type="primary" size="medium" icon="add-circle" @click="">
            Create role
          </Button>
        </template>
      </TableInteractive>
    </ContentBlock>
  </Container>
</template>
