<template>
  <LayoutModal :title="title" :show="isModalShown" @close="handleClose">
    <template #body>
      <div class="container" v-if="loaded && tableMetadata">
        <div class="row">
          <div
            class="overflow-auto mr-n3"
            :class="{ 'col-10': showChapters, 'col-12': !showChapters }"
            style="max-height: calc(100vh - 200px)"
          >
            <RowEdit
              v-if="schemaMetadata"
              :id="id"
              v-model="rowData"
              :pkey="pkey"
              :tableId="tableId"
              :tableMetaData="tableMetadata"
              :schemaMetaData="schemaMetadata"
              :visibleColumns="
                myUseChapters
                  ? columnsSplitByHeadings[currentPage - 1]
                  : visibleColumns
              "
              :clone="clone"
              :errorPerColumn="rowErrors"
              :applyDefaultValues="applyDefaultValues"
              @update:model-value="checkForErrors"
            />
          </div>
          <div
            v-if="showChapters"
            class="col-2 border-left chapter-menu overflow-auto"
            style="max-height: calc(100vh - 200px)"
          >
            <div class="mb-1">
              <b>Chapters</b>
            </div>
            <div v-for="(chapter, index) in columnsSplitByHeadings">
              <Tooltip
                :name="`chapter-${chapter[0]}-error-tooltip`"
                :value="
                  chapterStyleAndErrors[index].errorFields.length
                    ? `errors in:\n${chapterStyleAndErrors[index].errorFields}`
                    : ''
                "
                placement="left"
              >
                <button
                  type="button"
                  class="btn btn-link"
                  :class="{ 'font-weight-bold': index + 1 === currentPage }"
                  @click="setCurrentPage(index + 1)"
                  :style="chapterStyleAndErrors[index].style"
                >
                  {{ getHeadingLabel(chapter[0]) }}
                </button>
              </Tooltip>
            </div>
          </div>
        </div>
      </div>
    </template>
    <template #footer>
      <RowEditFooter
        :id="id + '-footer'"
        :tableLabel="label"
        :errorMessage="errorMessage"
        :saveDisabledMessage="saveDisabledMessage"
        :saveDraftDisabledMessage="saveDraftDisabledMessage"
        @cancel="handleClose"
        @saveDraft="handleSaveDraftRequest"
        @save="handleSaveRequest"
      >
        <div class="mr-auto">
          <div v-if="showChapters">
            <ButtonAction
              @click="setCurrentPage(currentPage - 1)"
              :disabled="currentPage <= 1"
              class="mr-2 pr-3"
            >
              <i :class="'fas fa-fw fa-chevron-left'" /> Previous
            </ButtonAction>
            <ButtonAction
              @click="setCurrentPage(currentPage + 1)"
              :disabled="currentPage >= columnsSplitByHeadings.length"
              class="pl-3"
            >
              Next <i :class="'fas fa-fw fa-chevron-right'" />
            </ButtonAction>
          </div>
        </div>
      </RowEditFooter>
    </template>
  </LayoutModal>
</template>

<script setup lang="ts">
import type {
  IColumn,
  ISchemaMetaData,
  ISetting,
  ITableMetaData,
} from "metadata-utils";
import { computed, onMounted, ref, toRefs } from "vue";
import type { IRow } from "../../Interfaces/IRow";
import { INewClient } from "../../client/IClient";
import Client from "../../client/client";
import constants from "../constants";
import LayoutModal from "../layout/LayoutModal.vue";
import { deepClone } from "../utils";
import ButtonAction from "./ButtonAction.vue";
import RowEdit from "./RowEdit.vue";
import RowEditFooter from "./RowEditFooter.vue";
import Tooltip from "./Tooltip.vue";
import {
  filterVisibleColumns,
  getChapterStyle,
  getRowErrors,
  getSaveDisabledMessage,
  isColumnVisible,
  removeKeyColumns,
  splitColumnIdsByHeadings,
} from "./formUtils/formUtils";

const props = withDefaults(
  defineProps<{
    id: string;
    tableId: string;
    isModalShown: boolean;
    schemaId: string;
    pkey?: Record<string, any>;
    clone?: boolean;
    visibleColumns?: string[];
    useChapters?: boolean | null;
    defaultValue?: Record<string, any> | null;
    applyDefaultValues?: boolean;
  }>(),
  { clone: false, defaultValue: null, useChapters: null }
);

const {
  applyDefaultValues,
  clone,
  defaultValue,
  id,
  pkey,
  schemaId,
  tableId,
  useChapters,
  visibleColumns,
} = props;

const { isModalShown } = toRefs(props);

const { IS_CHAPTERS_ENABLED_FIELD_NAME } = constants;
const rowData = ref<Record<string, any>>({});
const rowErrors = ref<Record<string, string>>({});
const tableMetadata = ref<ITableMetaData>();
const schemaMetadata = ref<ISchemaMetaData>();
const client = ref<INewClient>();
const errorMessage = ref<string>("");
const loaded = ref<boolean>(false);
const currentPage = ref<number>(1);
const myUseChapters = ref<boolean>();
const saveDisabledMessage = ref<string>("");

const emit = defineEmits(["update:newRow", "close"]);

const label = computed(() => tableMetadata.value?.label);

const titlePrefix = computed(() => {
  if (pkey && clone) {
    return "copy";
  } else if (pkey) {
    return "update";
  } else {
    return "insert";
  }
});

const title = computed(
  () => `${titlePrefix.value} into table: ${label.value} (${tableId})`
);

const columnsSplitByHeadings = computed(() => {
  const filteredByVisibilityFilters = filterVisibleColumns(
    tableMetadata.value?.columns || [],
    visibleColumns
  );
  const filteredByVisibilityExpressions = filteredByVisibilityFilters.filter(
    (column: IColumn) => {
      if (tableMetadata.value) {
        try {
          return isColumnVisible(column, rowData.value, tableMetadata.value);
        } catch (error: any) {
          rowErrors.value[column.id] = error;
          return true;
        }
      }
    }
  );
  const withoutMetadataColumns = filteredByVisibilityExpressions.filter(
    (column: IColumn) => !column.id.startsWith("mg_")
  );
  const splitByHeadings = splitColumnIdsByHeadings(withoutMetadataColumns);
  const filteredEmptyHeadings = splitByHeadings.filter(
    (chapter: string[]) => chapter.length > 1
  );
  return filteredEmptyHeadings;
});

const chapterStyleAndErrors = computed(() => {
  return columnsSplitByHeadings.value.map((page: string[]): IChapterInfo => {
    const errorFields = page.filter((fieldsInPage: string) =>
      Boolean(rowErrors.value[fieldsInPage])
    );
    return {
      style: getChapterStyle(page, rowErrors.value),
      errorFields,
    };
  });
});

const saveDraftDisabledMessage = computed(() => {
  const hasPrimaryKeyValue = tableMetadata.value?.columns.some(
    (column) =>
      column.key === 1 &&
      column.columnType !== "AUTO_ID" &&
      !rowData.value[column.id]
  );
  if (hasPrimaryKeyValue) {
    return "Cannot save draft: primary key is required";
  } else {
    return "";
  }
});

const showChapters = computed(() => {
  return myUseChapters.value && columnsSplitByHeadings.value.length > 1;
});

onMounted(async () => {
  loaded.value = false;
  client.value = Client.newClient(schemaId);
  schemaMetadata.value = await client.value.fetchSchemaMetaData();
  const settings: ISetting[] = await client.value.fetchSettings();

  if (useChapters === null) {
    myUseChapters.value =
      settings.find(
        (item: ISetting) => item.key === IS_CHAPTERS_ENABLED_FIELD_NAME
      )?.value !== "false";
  } else {
    myUseChapters.value = !!useChapters;
  }

  tableMetadata.value = await client.value.fetchTableMetaData(tableId);

  if (pkey) {
    const newRowData = await fetchRowData();

    if (clone) {
      rowData.value = removeKeyColumns(tableMetadata.value, newRowData);
    } else {
      rowData.value = newRowData;
    }
  }

  rowData.value = { ...rowData.value, ...deepClone(defaultValue) };
  checkForErrors();
  loaded.value = true;
});

function setCurrentPage(newPage: number) {
  currentPage.value = newPage;
}

function handleSaveRequest() {
  save({ ...rowData.value, mg_draft: false });
}

function handleSaveDraftRequest() {
  save({ ...rowData.value, mg_draft: true });
}

async function save(formData: IRow) {
  errorMessage.value = "";
  let result;

  const dataWithoutRefbacks = getDataWithoutRefbacks(
    formData,
    tableMetadata.value
  );

  if (pkey && !clone) {
    result = await client.value
      ?.updateDataRow(dataWithoutRefbacks, tableId, schemaId)
      .catch(handleSaveError);
  } else {
    result = await client.value
      ?.insertDataRow(dataWithoutRefbacks, tableId, schemaId)
      .catch(handleSaveError);
  }
  if (result) {
    emit("update:newRow", formData);
    handleClose();
  }
}

function getDataWithoutRefbacks(
  formData: IRow,
  tableMetadata: ITableMetaData | undefined
): IRow {
  if (!tableMetadata) {
    return formData;
  } else {
    let dataCopy = { ...formData };
    tableMetadata.columns.forEach((column: IColumn) => {
      if (column.columnType === "REFBACK") {
        delete dataCopy[column.id];
      }
    });
    return dataCopy;
  }
}

function handleSaveError(error: any) {
  if (error.response?.status === 403) {
    errorMessage.value =
      "Schema doesn't exist or permission denied. Do you need to Sign In?";
  } else {
    errorMessage.value =
      error.response?.data?.errors[0]?.message ||
      "An Error occurred during save";
  }
}
async function fetchRowData() {
  if (pkey) {
    const result = await client.value?.fetchRowData(tableId, pkey);
    if (!result) {
      errorMessage.value = `Error, unable to fetch data for this row (${pkey})`;
    } else {
      return result;
    }
  } else {
    errorMessage.value = "No row key provided";
  }
}

function handleClose() {
  errorMessage.value = "";
  emit("close");
}

function checkForErrors() {
  if (tableMetadata.value) {
    rowErrors.value = getRowErrors(tableMetadata.value, rowData.value);
  }
  saveDisabledMessage.value = getSaveDisabledMessage(rowErrors.value);
}

function getHeadingLabel(headingId: string) {
  const column = tableMetadata.value?.columns.find(
    (column: IColumn) => column.id === headingId
  );
  return column?.label || column?.id || headingId;
}

interface IChapterInfo {
  style: { color?: "red" };
  errorFields: string[];
}
</script>

<style scoped>
.chapter-menu {
  padding: 1rem;
  margin: -1rem -1rem -1rem 1rem;
  max-width: 16rem;
  overflow: hidden;
}

.chapter-menu button {
  padding: 0;
  max-width: 14rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
}
</style>

<docs>
<template>
  <DemoItem label="Edit Modal">
    <p>
      This component can be used in chapter mode split the form into multiple
      chapter based on headings.
    </p>
    <div class="mt-2 mb-3">
      Use the "isChaptersEnabled" schema setting to switch mode.
      <div class="font-weight-bold">
        isChaptersEnabled:
        <input
          :disabled="loadFromBackend"
          type="checkbox"
          id="checkbox"
          v-model="useChapters"
        />
      </div>
      <div class="font-weight-bold">
        load from backend:
        <input type="checkbox" id="checkbox" v-model="loadFromBackend" />
      </div>
    </div>

    <button class="btn btn-primary" @click="isModalShown = !isModalShown">
      Show {{ demoMode }} {{ tableId }}
    </button>
    <label for="table-selector" class="ml-5 pr-1">table</label>
    <select id="table-selector" v-model="tableId">
      <option>Pet</option>
      <option>Order</option>
      <option>Category</option>
      <option>User</option>
    </select>
    <input
      type="radio"
      id="insert"
      value="insert"
      v-model="demoMode"
      class="ml-5"
    />
    <label for="insert" class="pl-1">Insert</label>
    <input
      type="radio"
      id="update"
      value="update"
      v-model="demoMode"
      class="ml-1 pr-1"
    />
    <label for="update" class="pl-1">Update</label>
    <input
      type="radio"
      id="clone"
      value="clone"
      v-model="demoMode"
      class="ml-1 pr-1"
    />
    <label for="clone" class="pl-1">Clone</label>
    <EditModal
      :key="tableId + demoKey + demoMode + useChapters"
      id="edit-modal"
      :tableId="tableId"
      :pkey="demoKey"
      :clone="demoMode === 'clone'"
      :isModalShown="isModalShown"
      :schemaId="schemaId"
      :useChapters="useChapters"
      @close="isModalShown = false"
    />
  </DemoItem>
</template>

<script>
export default {
  data: function () {
    return {
      schemaId: "pet store",
      tableId: "Pet",
      demoMode: "insert", // one of [insert, update, clone]
      demoKey: null, // empty in case of insert
      isModalShown: false,
      useChapters: true,
      loadFromBackend: false,
    };
  },
  methods: {
    async reload() {
      const client = this.$Client.newClient(this.schemaId);
      const rowData = await client.fetchTableDataValues(this.tableId, {});
      this.demoKey =
        this.demoMode === "insert"
          ? null
          : await client.convertRowToPrimaryKey(rowData[0], this.tableId);
      const settings = await client.fetchSettings();
      if (this.loadFromBackend) {
        this.useChapters =
          settings.find(
            (item) =>
              item.key === this.$constants.IS_CHAPTERS_ENABLED_FIELD_NAME
          )?.value !== "false";
      }
    },
  },
  watch: {
    demoMode() {
      this.reload();
    },
    useChapters() {
      this.reload();
    },
    loadFromBackend() {
      this.reload();
    },
  },
  mounted() {
    this.reload();
  },
};
</script>
</docs>
