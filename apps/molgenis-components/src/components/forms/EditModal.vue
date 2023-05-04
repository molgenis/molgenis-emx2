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
        :isSaveDisabled="isSaveDisabled"
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

<script>
import Client from "../../client/client.ts";
import LayoutModal from "../layout/LayoutModal.vue";
import RowEditFooter from "./RowEditFooter.vue";
import EditModalWizard from "./EditModalWizard.vue";
import RowEdit from "./RowEdit.vue";
import ButtonAction from "./ButtonAction.vue";
import { filterObject, deepClone, getLocalizedLabel } from "../utils.ts";
import constants from "../constants";

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
      tableMetaData: null,
      schemaMetaData: null,
      client: null,
      errorMessage: null,
      loaded: true,
      currentPage: 1,
      pageCount: 1,
      useChapters: true,
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
      required: false,
    },
    pkey: {
      type: Object,
      required: false,
      default: () => null,
    },
    clone: {
      type: Boolean,
      required: false,
      default: () => false,
    },
    visibleColumns: {
      type: Array,
      required: false,
      default: () => null,
    },
    defaultValue: {
      type: Object,
      required: false,
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
      const headings = this.tableMetaData.columns
        .filter((column) => column.columnType === "HEADING")
        .map((column) => column.name);
      if (this.tableMetaData.columns[0].columnType === "HEADING") {
        return headings;
      } else {
        return ["First chapter"].concat(headings);
      }
    },
    titlePrefix() {
      return this.pkey && this.clone ? "copy" : this.pkey ? "update" : "insert";
    },
    isSaveDisabled() {
      return this.pageCount > 1 ? this.pageCount !== this.currentPage : false;
    },
  },
  methods: {
    setCurrentPage(newPage) {
      this.currentPage = newPage;
    },
    handleSaveRequest() {
      this.save({ ...this.rowData, mg_draft: false });
    },
    handleSaveDraftRequest() {
      this.save({ ...this.rowData, mg_draft: true });
    },
    async save(formData) {
      this.errorMessage = null;
      const action =
        this.pkey && !this.clone ? "updateDataRow" : "insertDataRow";
      const result = await this.client[action](
        formData,
        this.tableName,
        this.schemaName
      ).catch(this.handleSaveError);
      if (result) {
        this.handleClose();
      }
    },
    handleSaveError(error) {
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
      const result = await this.client.fetchRowData(this.tableName, this.pkey);
      if (!result) {
        this.errorMessage = `Error, unable to fetch data for this row (${this.pkey})`;
      } else {
        return result;
      }
    },
    handleClose() {
      this.errorMessage = null;
      this.$emit("close");
    },
  },
  async mounted() {
    this.loaded = false;
    this.client = Client.newClient(this.schemaName);
    this.schemaMetaData = await this.client.fetchSchemaMetaData();
    const settings = await this.client.fetchSettings();

    this.useChapters =
      settings.find((item) => item.key === IS_CHAPTERS_ENABLED_FIELD_NAME)
        ?.value !== "false";

    this.tableMetaData = await this.client.fetchTableMetaData(this.tableName);

    if (this.pkey) {
      this.rowData = await this.fetchRowData();

      if (this.clone) {
        // in case of clone, remove the key columns from the row data
        const keyColumnsNames = this.tableMetaData.columns
          .filter((column) => column.key === 1)
          .map((column) => column.name);

        this.rowData = filterObject(
          this.rowData,
          (key) => !keyColumnsNames.includes(key)
        );
      }
    }

    this.rowData = { ...this.rowData, ...deepClone(this.defaultValue) };
    this.loaded = true;
  },
};
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
      const rowData = await client.fetchTableDataValues(this.tableName);
      this.demoKey = await this.$utils.requestPrimaryKey(rowData[0], this.tableName, this.schemaName);
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
