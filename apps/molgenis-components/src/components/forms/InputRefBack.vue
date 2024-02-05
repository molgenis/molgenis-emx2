<template>
  <FormGroup v-bind="$props">
    <Spinner v-if="isLoading" />
    <TableMolgenis
      v-else-if="hasPrimaryKey"
      :schemaId="schemaId"
      :data="data"
      :columns="visibleColumns"
      :table-metadata="tableMetadata"
      style="overflow-x: scroll"
    >
      <template v-slot:rowcolheader>
        <slot
          name="rowcolheader"
          v-bind="$props"
          :canEdit="canEdit"
          :reload="reload"
          :schemaId="schemaId"
        />
        <RowButton
          v-if="canEdit"
          type="add"
          @add="handleRowAction('add')"
          class="d-inline p-0"
        />
      </template>
      <template v-slot:rowheader="slotProps">
        <slot
          name="rowheader"
          :row="slotProps.row"
          :metadata="tableMetadata"
          :rowKey="slotProps.rowKey"
        />
        <RowButton
          v-if="canEdit"
          type="edit"
          :table="tableId"
          :schemaId="schemaId"
          :visible-columns="visibleColumnIds"
          :pkey="slotProps.rowKey"
          @close="reload"
          @edit="handleRowAction('edit', slotProps.rowKey)"
        />
        <RowButton
          v-if="canEdit"
          type="clone"
          :table="tableId"
          :schemaId="schemaId"
          :pkey="slotProps.rowKey"
          :visible-columns="visibleColumnIds"
          @close="reload"
          @clone="handleRowAction('clone', slotProps.rowKey)"
        />
        <RowButton
          v-if="canEdit"
          type="delete"
          @delete="handleDeleteRowRequest(slotProps.rowKey)"
        />
      </template>
    </TableMolgenis>
    <MessageWarning v-else>
      This can only be filled in after you have saved (or saved draft).
    </MessageWarning>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>

    <EditModal
      v-if="isEditModalShown"
      :isModalShown="true"
      :id="tableId + '-edit-modal'"
      :tableId="tableId"
      :pkey="editRowPrimaryKey"
      :visibleColumns="visibleColumnIds"
      :clone="editMode === 'clone'"
      :schemaId="schemaId"
      :defaultValue="defaultValue"
      @close="handleModalClose"
    />

    <ConfirmModal
      v-if="isDeleteModalShown"
      :title="'Delete from ' + tableId"
      actionLabel="Delete"
      actionType="danger"
      :tableId="tableId"
      :tableLabel="tableMetadata.label"
      :pkey="editRowPrimaryKey"
      @close="isDeleteModalShown = false"
      @confirmed="handleExecuteDelete"
    />
  </FormGroup>
</template>

<script>
import Client from "../../client/client.ts";
import Spinner from "../layout/Spinner.vue";
import RowButton from "../tables/RowButton.vue";
import TableMolgenis from "../tables/TableMolgenis.vue";
import { deepEqual } from "../utils";
import ConfirmModal from "./ConfirmModal.vue";
import FormGroup from "./FormGroup.vue";
import MessageError from "./MessageError.vue";
import MessageWarning from "./MessageWarning.vue";
import BaseInput from "./baseInputs/BaseInput.vue";

export default {
  name: "InputRefBack",
  extends: BaseInput,
  components: {
    FormGroup,
    TableMolgenis,
    RowButton,
    Spinner,
    MessageWarning,
    ConfirmModal,
    MessageError,
  },
  props: {
    /** name of the table from which is referred back to this field */
    tableId: {
      type: String,
      required: true,
    },
    /** id of the column in the other table */
    refBackId: {
      type: String,
      required: true,
    },
    /**
     * primary key of the current table that refback should point to
     * when empty ( in case of draft , add message is shown instead of the table)
     *  */
    pkey: {
      type: [Object, null],
      required: true,
    },
    schemaId: {
      type: String,
      required: false,
    },
    /**
     * if table (that has a column that is referred to by this table) can be edited
     *  */
    canEdit: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  data() {
    return {
      client: null,
      tableMetadata: null,
      data: null,
      isLoading: false,
      isEditModalShown: false,
      isDeleteModalShown: false,
      editMode: "add", // add, edit, clone
      editRowPrimaryKey: null,
      graphqlError: null,
      defaultValue: null,
    };
  },
  computed: {
    graphqlFilter() {
      return {
        [this.refBackId]: {
          equals: this.pkey,
        },
      };
    },
    visibleColumnIds() {
      return this.visibleColumns.map((c) => c.id);
    },
    visibleColumns() {
      //columns, excludes refback and mg_
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.filter(
          (c) => c.id != this.refBackId && !c.id.startsWith("mg_")
        );
      }
      return [];
    },
    hasPrimaryKey() {
      return this.pkey ? Boolean(Object.values(this.pkey).length) : false;
    },
  },
  methods: {
    async reload() {
      this.isLoading = true;
      this.data = await this.client.fetchTableDataValues(this.tableId, {
        filter: this.graphqlFilter,
      });
      this.isLoading = false;
    },
    handleRowAction(type, key) {
      this.editMode = type;
      this.editRowPrimaryKey = key;
      this.isEditModalShown = true;
    },
    handleModalClose() {
      this.isEditModalShown = false;
      this.reload();
    },
    handleDeleteRowRequest(key) {
      this.editRowPrimaryKey = key;
      this.isDeleteModalShown = true;
    },
    async handleExecuteDelete() {
      this.isDeleteModalShown = false;
      await this.client
        .deleteRow(this.editRowPrimaryKey, this.tableId)
        .catch(this.handleError)
        .then(this.reload());
      let newValue = this.modelValue.filter(
        (v) => !deepEqual(v, this.editRowPrimaryKey)
      );
      this.$emit("update:modelValue", newValue);
    },
    handleError(error) {
      if (Array.isArray(error?.response?.data?.errors)) {
        this.graphqlError = error.response.data.errors[0].message;
      } else {
        this.graphqlError = error;
      }
      this.loading = false;
    },
  },
  mounted: async function () {
    this.client = Client.newClient(this.schemaId);
    this.isLoading = true;
    this.tableMetadata = await this.client
      .fetchTableMetaData(this.tableId)
      .catch((error) => (this.graphqlError = error.message));
    this.defaultValue = new Object();
    this.defaultValue[this.refBackId] = await this.pkey;
    await this.reload();
  },
};
</script>

<docs>

<template>
  <div>
    <p>
      note, this input doesn't have value on its own, it just allows you to edit the refback in context.
      This also means you cannot do this unless your current record has a pkey to point to
    </p>

    <div class="my-3">
      <label for="refback1">When row has not been saved (key === null) </label>
      <InputRefBack
          id="refback1"
          label="Orders"
          tableId="Order"
          refBackId="pet"
          :pkey=null
          schemaId="pet store"
      />
    </div>

    <div class="my-3">
      <label for="refback1">When row has not been saved (key === {}) </label>
      <InputRefBack
          id="refback1"
          label="Orders"
          tableId="Order"
          refBackId="pet"
          :pkey={}
          schemaName="pet store"
      />
    </div>

    <div class="my-3">
      <label for="refback2">When row has a key but can not be edited </label>

      <InputRefBack
          id="refback2"
          label="Orders"
          tableId="Order"
          refBackId="pet"
          :pkey="{name:'spike'}"
          schemaId="pet store"
      />
    </div>

    <div class="my-3">
      <label for="refback3">When row has a key and can be edited </label>
      <InputRefBack
          id="refback3"
          canEdit
          label="Orders"
          tableId="Order"
          refBackId="pet"
          :pkey="{name:'spike'}"
          schemaId="pet store"
      />
    </div>
  </div>

</template>
</docs>
