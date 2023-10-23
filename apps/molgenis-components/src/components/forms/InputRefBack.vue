<template>
  <FormGroup v-bind="$props">
    <Spinner v-if="isLoading" />
    <TableMolgenis
      v-else-if="refTablePrimaryKeyObject"
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
          :refTablePrimaryKeyObject="slotProps.rowKey"
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
      :visibleColumns="visibleColumns"
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
      :pkey="editRowPrimaryKey"
      @close="isDeleteModalShown = false"
      @confirmed="handleExecuteDelete"
    />
  </FormGroup>
</template>

<script>
import Client from "../../client/client.ts";
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import TableMolgenis from "../tables/TableMolgenis.vue";
import RowButton from "../tables/RowButton.vue";
import MessageWarning from "./MessageWarning.vue";
import MessageError from "./MessageError.vue";
import Spinner from "../layout/Spinner.vue";
import ConfirmModal from "./ConfirmModal.vue";
import { convertToCamelCase, deepEqual } from "../utils";

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
    /** name of the column in the other table */
    refBack: {
      type: String,
      required: true,
    },
    /**
     * primary key of the current table that refback should point to
     * when empty ( in case of draft , add message is shown instead of the table)
     *  */
    refTablePrimaryKeyObject: {
      type: Object,
      required: false,
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
      var result = new Object();
      result[convertToCamelCase(this.refBack)] = {
        equals: this.refTablePrimaryKeyObject,
      };
      return result;
    },
    visibleColumnIds() {
      return this.visibleColumns.map((c) => c.id);
    },
    visibleColumns() {
      //columns, excludes refback and mg_
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.filter(
          (c) => c.id != this.refBack && !c.id.startsWith("mg_")
        );
      }
      return [];
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
      .catch((error) => (this.errorMessage = error.message));
    this.defaultValue = new Object();
    this.defaultValue[convertToCamelCase(this.refBack)] = await this
      .refTablePrimaryKeyObject;
    await this.reload();
  },
};
</script>

<docs>

<template>
  <div>
    <p>
      note, this input doesn't have value on its own, it just allows you to edit the refback in context.
      This also means you cannot do this unless your current record has a refTablePrimaryKeyObject to point to
    </p>

    <div class="my-3">
      <label for="refback1">When row has not been saved ( has not key) </label>
      <InputRefBack
          id="refback1"
          label="Orders"
          tableId="Order"
          refBack="pet"
          :refTablePrimaryKeyObject=null
          schemaId="pet store"
      />
    </div>


    <div class="my-3">
      <label for="refback2">When row has a key but can not be edited </label>

      <InputRefBack
          id="refback2"
          label="Orders"
          tableId="Order"
          refBack="pet"
          :refTablePrimaryKeyObject="{name:'spike'}"
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
          refBack="pet"
          :refTablePrimaryKeyObject="{name:'spike'}"
          schemaId="pet store"
      />
    </div>
  </div>

</template>
</docs>
