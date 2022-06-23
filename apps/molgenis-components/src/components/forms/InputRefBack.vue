<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <Spinner v-if="!this.tableMetadata || !this.data" />
    <TableMolgenis
      v-else-if="refTablePrimaryKeyObject"
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
          :grapqlURL="graphqlURL"
        />
        <RowButton
          v-if="canEdit"
          type="add"
          :table="tableName"
          :graphqlURL="graphqlURL"
          :visible-columns="visibleColumnNames"
          :default-value="defaultValue"
          @close="reload"
          class="d-inline p-0"
        />
      </template>
      <template v-slot:rowheader="slotProps">
        <slot
          name="rowheader"
          :row="slotProps.row"
          :metadata="tableMetadata"
          :rowkey="slotProps.rowkey"
        />
        <RowButton
          v-if="canEdit"
          type="edit"
          :table="tableName"
          :graphqlURL="graphqlURL"
          :visible-columns="visibleColumnNames"
          :refTablePrimaryKeyObject="getPrimaryKey(slotProps.row, tableMetadata)"
          @close="reload"
        />
        <RowButton
          v-if="canEdit"
          type="clone"
          :table="tableName"
          :graphqlURL="graphqlURL"
          :pkey="getPrimaryKey(slotProps.row, tableMetadata)"
          @close="reload"
          :visible-columns="visibleColumnNames"
          :default-value="defaultValue"
        />
        <RowButton
          v-if="canEdit"
          type="delete"
          :table="tableName"
          :graphqlURL="graphqlURL"
          :pkey="getPrimaryKey(slotProps.row, tableMetadata)"
          @close="reload"
        />
      </template>
    </TableMolgenis>
    <MessageWarning v-else>
      This can only be filled in after you have saved (or saved draft).
    </MessageWarning>
  </FormGroup>
</template>

<script>
import Client from "../../client/client.js";
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import TableMolgenis from "../tables/TableMolgenis.vue";
import RowButton from "../tables/RowButton.vue";
import MessageWarning from "./MessageWarning.vue";
import Spinner from "../layout/Spinner.vue";
import { getPrimaryKey } from "../utils";

export default {
  name: "InputRefBack",
  extends: BaseInput,
  components: {
    FormGroup,
    TableMolgenis,
    RowButton,
    Spinner,
    MessageWarning,
  },
  props: {
    /** name of the table from which is referred back to this field */
    tableName: {
      type: String,
      required: true
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
    graphqlURL: {
      type: String,
      default: "graphql",
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
      data: null
    }
  },
  computed: {
    defaultValue() {
      var result = new Object();
      result[this.refBack] = this.refTablePrimaryKeyObject;
      return result;
    },
    graphqlFilter() {
      var result = new Object();
      result[this.refBack] = {
        equals: this.refTablePrimaryKeyObject,
      };
      return result;
    },
    visibleColumnNames() {
      return this.visibleColumns.map((c) => c.name);
    },
    visibleColumns() {
      //columns, excludes refback and mg_
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.filter(
          (c) => c.name != this.refBack && !c.name.startsWith("mg_")
        );
      }
      return [];
    }
  },
  methods: {
    getPrimaryKey,
    async reload () {
      this.data = await this.client.fetchTableDataValues(this.tableName, { filter: this.graphqlFilter });
    }
  },
  mounted: async function () {
    this.client = Client.newClient(this.graphqlURL);
    this.tableMetadata = await this.client.fetchTableMetaData(this.tableName);
    this.data = await this.client.fetchTableDataValues(this.tableName, { filter: this.graphqlFilter });
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
      tableName="Order"
      refBack="pet"
      :refTablePrimaryKeyObject=null
      graphqlURL="/pet store/graphql"
  />
  </div>


  <div class="my-3">
  <label for="refback2">When row has a key but can not be edited </label>

  <InputRefBack
      id="refback2"
      label="Orders"
      tableName="Order"
      refBack="pet"
      :refTablePrimaryKeyObject="{name:'spike'}"
      graphqlURL="/pet store/graphql"
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
      :refTablePrimaryKeyObject="{name:'spike'}"
      graphqlURL="/pet store/graphql"
  />
  </div>
</div>

</template>
</docs>
