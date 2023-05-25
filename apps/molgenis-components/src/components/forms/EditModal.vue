<template>
  <LayoutModal :title="title" :show="isModalShown" @close="handleClose">
    <template #body>
      <div class="d-flex" v-if="loaded && tableMetaData">
        <EditModalWizard
          v-if="useChapters"
          :id="id"
          v-model="rowData"
          :pkey="pkey"
          :tableName="tableName"
          :tableMetaData="tableMetaData"
          :schemaMetaData="schemaMetaData"
          :visibleColumns="visibleColumns"
          :clone="clone"
          :page="currentPage"
          @setPageCount="pageCount = $event"
          @numberOfErrorsInForm="handleNumberOfErrors"
          :locale="locale"
          class="flex-grow-1"
        />
        <RowEdit
          v-else
          :id="id"
          v-model="rowData"
          :pkey="pkey"
          :tableName="tableName"
          :tableMetaData="tableMetaData"
          :schemaMetaData="schemaMetaData"
          :visibleColumns="visibleColumns"
          :clone="clone"
          :locale="locale"
          class="flex-grow-1"
          @numberOfErrorsInForm="handleNumberOfErrors"
        />
        <div v-if="pageCount > 1" class="border-left chapter-menu">
          <div class="mb-1"><b>Chapters</b></div>
          <div v-for="(heading, index) in pageHeadings">
            <button
              type="button"
              class="btn btn-link"
              :title="heading"
              :class="{ 'font-weight-bold': index + 1 === currentPage }"
              @click="setCurrentPage(index + 1)"
            >
              {{ heading }}
            </button>
          </div>
        </div>
      </div>
    </template>
    <template #footer>
      <RowEditFooter
        :id="id + '-footer'"
        :tableName="tableName"
        :errorMessage="errorMessage"
        :saveDisabledMessage="saveDisabledMessage"
        @cancel="handleClose"
        @saveDraft="handleSaveDraftRequest"
        @save="handleSaveRequest"
      >
        <div class="mr-auto">
          <div v-if="pageCount > 1">
            <ButtonAction
              @click="setCurrentPage(currentPage - 1)"
              :disabled="currentPage <= 1"
              class="mr-2 pr-3"
            >
              <i :class="'fas fa-fw fa-chevron-left'" /> Previous
            </ButtonAction>
            <ButtonAction
              @click="setCurrentPage(currentPage + 1)"
              :disabled="currentPage >= pageCount"
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

<script lang="ts">
import { IColumn } from "../../Interfaces/IColumn";
import { ISchemaMetaData } from "../../Interfaces/IMetaData";
import { IRow } from "../../Interfaces/IRow";
import { ISetting } from "../../Interfaces/ISetting";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import { INewClient } from "../../client/IClient";
import Client from "../../client/client";
import constants from "../constants";
import LayoutModal from "../layout/LayoutModal.vue";
import { deepClone, filterObject, getLocalizedLabel } from "../utils";
import ButtonAction from "./ButtonAction.vue";
import EditModalWizard from "./EditModalWizard.vue";
import RowEdit from "./RowEdit.vue";
import RowEditFooter from "./RowEditFooter.vue";

const { IS_CHAPTERS_ENABLED_FIELD_NAME } = constants;

export default {
  name: "EditModal",
  components: {
    LayoutModal,
    RowEditFooter,
    RowEdit,
    EditModalWizard,
    ButtonAction,
  },
  data() {
    return {
      rowData: {},
      tableMetaData: null as unknown as ITableMetaData,
      schemaMetaData: null as unknown as ISchemaMetaData,
      client: null as unknown as INewClient,
      errorMessage: "",
      loaded: true,
      currentPage: 1,
      pageCount: 1,
      useChapters: true,
      saveDisabledMessage: "",
    };
  },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableName: {
      type: String,
      required: true,
    },
    isModalShown: {
      type: Boolean,
      required: true,
    },
    schemaName: {
      type: String,
      required: true,
    },
    pkey: {
      type: Object,
      default: () => null,
    },
    clone: {
      type: Boolean,
      default: () => false,
    },
    visibleColumns: {
      type: Array,
      default: () => null,
    },
    defaultValue: {
      type: Object,
      default: () => null,
    },
    locale: {
      type: String,
      default: () => "en",
    },
  },
  computed: {
    title() {
      return `${this.titlePrefix} into table: ${this.label} (${this.tableName})`;
    },
    label() {
      if (this.tableMetaData) {
        return getLocalizedLabel(this.tableMetaData);
      }
    },
    pageHeadings() {
      return getPageHeadings(this.tableMetaData);
    },
    titlePrefix() {
      return this.pkey && this.clone ? "copy" : this.pkey ? "update" : "insert";
    },
  },
  methods: {
    setCurrentPage(newPage: number) {
      this.currentPage = newPage;
    },
    handleSaveRequest() {
      this.save({ ...this.rowData, mg_draft: false });
    },
    handleSaveDraftRequest() {
      this.save({ ...this.rowData, mg_draft: true });
    },
    async save(formData: IRow) {
      this.errorMessage = "";
      let result;
      if (this.pkey && !this.clone) {
        result = await this.client
          .updateDataRow(formData, this.tableName, this.schemaName)
          .catch(this.handleSaveError);
        if (result) {
          this.handleClose();
        }
      } else {
        result = await this.client
          .insertDataRow(formData, this.tableName, this.schemaName)
          .catch(this.handleSaveError);
        if (result) {
          this.handleClose();
        }
      }
    },
    handleSaveError(error: any) {
      if (error.response?.status === 403) {
        this.errorMessage =
          "Schema doesn't exist or permission denied. Do you need to Sign In?";
      } else {
        this.errorMessage =
          error.response?.data?.errors[0]?.message ||
          "An Error occurred during save";
      }
    },
    async fetchRowData() {
      const result = await this.client?.fetchRowData(this.tableName, this.pkey);
      if (!result) {
        this.errorMessage = `Error, unable to fetch data for this row (${this.pkey})`;
      } else {
        return result;
      }
    },
    handleClose() {
      this.errorMessage = "";
      this.$emit("close");
    },
    handleNumberOfErrors(event: number) {
      this.saveDisabledMessage =
        event > 0 ? `There are ${event} error(s) preventing saving` : "";
    },
  },
  async mounted() {
    this.loaded = false;
    this.client = Client.newClient(this.schemaName);
    this.schemaMetaData = await this.client.fetchSchemaMetaData();
    const settings: ISetting[] = await this.client.fetchSettings();

    this.useChapters =
      settings.find(
        (item: ISetting) => item.key === IS_CHAPTERS_ENABLED_FIELD_NAME
      )?.value !== "false";

    this.tableMetaData = await this.client.fetchTableMetaData(this.tableName);

    if (this.pkey) {
      this.rowData = await this.fetchRowData();

      if (this.clone) {
        // in case of clone, remove the key columns from the row data
        const keyColumnsNames = this.tableMetaData?.columns
          ?.filter((column: IColumn) => column.key === 1)
          .map((column: IColumn) => column.name);

        this.rowData = filterObject(
          this.rowData,
          (key) => !keyColumnsNames?.includes(key)
        );
      }
    }

    this.rowData = { ...this.rowData, ...deepClone(this.defaultValue) };
    this.loaded = true;
  },
};

function getPageHeadings(tableMetadata: ITableMetaData): string[] {
  const columns: IColumn[] = tableMetadata?.columns
    ? tableMetadata?.columns
    : [];
  const headings: string[] = columns
    .filter((column) => column.columnType === "HEADING")
    .map((column) => column.name);
  if (columns[0].columnType === "HEADING") {
    return headings;
  } else {
    return ["First chapter"].concat(headings);
  }
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
  <p>This component can be used in chapter mode split the form into multiple chapter based on headings. Use the "isChaptersEnabled" schema setting to switch mode. <br/>Current value: <pre style='display:inline'>{{useChapters}}</pre></p>
    <button class="btn btn-primary" @click="isModalShown = !isModalShown">
      Show {{ demoMode }} {{ tableName }}
    </button>
    <label for="table-selector" class="ml-5 pr-1">table</label>
    <select id="table-selector" v-model="tableName">
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
      :key="tableName + demoKey + demoMode"
      id="edit-modal"
      :tableName="tableName"
      :pkey="demoKey"
      :clone="demoMode === 'clone'"
      :isModalShown="isModalShown"
      :schemaName="schemaName"
      @close="isModalShown = false"
    />
  </DemoItem>
</template>

<script>
export default {
  data: function () {
    return {
      schemaName: "pet store",
      tableName: "Pet",
      demoMode: "insert", // one of [insert, update, clone]
      demoKey: null, // empty in case of insert
      isModalShown: false,
      useChapters: true
    };
  },
  methods: {
    async reload() {
      const client = this.$Client.newClient(this.schemaName);
      const tableMetaData = await client.fetchTableMetaData(this.tableName);
      const rowData = await client.fetchTableDataValues(this.tableName);
      this.demoKey = this.$utils.getPrimaryKey(rowData[0], tableMetaData);
      const settings = await client.fetchSettings();
      this.useChapters =
        settings.find((item) => item.key === IS_CHAPTERS_ENABLED_FIELD_NAME)?.value !==
        "false";
    },
    onModeChange() {
      if (this.demoMode !== "insert") {
        this.reload();
      } else {
        this.demoKey = null;
      }
    },
  },
  watch: {
    demoMode() {
      this.onModeChange();
    },
  },
};
</script>
</docs>
