<template>
  <FormGroup v-bind="$props">
    <Spinner v-if="isLoading" />
    <TableMolgenis
      v-else-if="hasPrimaryKey"
      :schemaName="schemaName"
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
          :schemaName="schemaName"
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
          :table="tableName"
          :schemaName="schemaName"
          :visible-columns="visibleColumnNames"
          :pkey="slotProps.rowKey"
          @close="reload"
          @edit="handleRowAction('edit', slotProps.rowKey)"
        />
        <RowButton
          v-if="canEdit"
          type="clone"
          :table="tableName"
          :schemaName="schemaName"
          :pkey="slotProps.rowKey"
          :visible-columns="visibleColumnNames"
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
      :id="tableName + '-edit-modal'"
      :tableName="tableName"
      :pkey="editRowPrimaryKey"
      :visibleColumns="visibleColumns"
      :clone="editMode === 'clone'"
      :schemaName="schemaName"
      :defaultValue="defaultValue"
      @close="handleModalClose"
    />

    <ConfirmModal
      v-if="isDeleteModalShown"
      :title="'Delete from ' + tableName"
      actionLabel="Delete"
      actionType="danger"
      :tableName="tableName"
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
import { convertToCamelCase, deepEqual } from "../utils";
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
    tableName: {
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
    pkey: {
      type: [Object, null],
      required: true,
    },
    schemaName: {
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
        [convertToCamelCase(this.refBack)]: {
          equals: this.pkey,
        },
      };
    },
    visibleColumnNames() {
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
    hasPrimaryKey() {
      return this.pkey ? Boolean(Object.values(this.pkey).length) : false;
    },
  },
  methods: {
    async reload() {
      this.isLoading = true;
      this.data = await this.client.fetchTableDataValues(this.tableName, {
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
        .deleteRow(this.editRowPrimaryKey, this.tableName)
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
    this.client = Client.newClient(this.schemaName);
    this.isLoading = true;
    this.tableMetadata = await this.client
      .fetchTableMetaData(this.tableName)
      .catch((error) => (this.errorMessage = error.message));
    this.defaultValue = new Object();
    this.defaultValue[convertToCamelCase(this.refBack)] = await this.pkey;
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
          tableName="Order"
          refBack="pet"
          :pkey=null
          schemaName="pet store"
      />
    </div>

    <div class="my-3">
      <label for="refback1">When row has not been saved (key === {}) </label>
      <InputRefBack
          id="refback1"
          label="Orders"
          tableName="Order"
          refBack="pet"
          :pkey={}
          schemaName="pet store"
      />
    </div>

    <div class="my-3">
      <label for="refback2">When row has a key but can not be edited </label>

      <InputRefBack
          id="refback2"
          label="Orders"
          tableName="Order"
          refBack="pet"
          :pkey="{name:'spike'}"
          schemaName="pet store"
      />
    </div>

    <div class="my-3">
      <label for="refback3">When row has a key and can be edited </label>
      <InputRefBack
          id="refback3"
          canEdit
          label="Orders"
          tableName="Order"
          refBack="pet"
          :pkey="{name:'spike'}"
          schemaName="pet store"
      />
    </div>
  </div>

</template>
</docs>
