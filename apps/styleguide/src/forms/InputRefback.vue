<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <Spinner v-if="!this.tableMetadata" />
    <TableMolgenis
      v-else-if="pkey"
      :data="data"
      :columns="visibleColumns"
      :table-metadata="tableMetadata"
    >
      <template v-slot:colheader>
        <slot
          name="colheader"
          v-bind="$props"
          :canEdit="canEdit"
          :reload="reload"
          :grapqlURL="graphqlURL"
        />
        <RowButtonAdd
          v-if="canEdit"
          :table="table"
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
        <RowButtonEdit
          v-if="canEdit"
          :table="table"
          :graphqlURL="graphqlURL"
          :visible-columns="visibleColumnNames"
          :pkey="getPkey(slotProps.row)"
          @close="reload"
        />
        <RowButtonClone
          v-if="canEdit"
          :table="table"
          :graphqlURL="graphqlURL"
          :pkey="getPkey(slotProps.row)"
          @close="reload"
          :visible-columns="visibleColumnNames"
          :default-value="defaultValue"
        />
        <RowButtonDelete
          v-if="canEdit"
          :table="table"
          :graphqlURL="graphqlURL"
          :pkey="getPkey(slotProps.row)"
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
import _baseInput from "./_baseInput";
import FormGroup from "./_formGroup";
import TableMolgenis from "../tables/TableMolgenis";
import TableMixin from "../mixins/TableMixin";
import RowButtonAdd from "../tables/RowButtonAdd";
import RowButtonDelete from "../tables/RowButtonDelete";
import RowButtonEdit from "../tables/RowButtonEdit";
import RowButtonClone from "../tables/RowButtonClone";
import MessageWarning from "./MessageWarning";
import Spinner from "../layout/Spinner";

export default {
  extends: _baseInput,
  mixins: [TableMixin],
  components: {
    FormGroup,
    TableMolgenis,
    RowButtonDelete,
    RowButtonEdit,
    RowButtonAdd,
    RowButtonClone,
    Spinner,
    MessageWarning,
  },
  props: {
    /** name of the column in the other table */
    refBack: String,
    /** pkey of the current table that refback should point to */
    pkey: Object,
  },
  computed: {
    defaultValue() {
      var result = new Object();
      result[this.refBack] = this.pkey;
      return result;
    },
    graphqlFilter() {
      var result = new Object();
      result[this.refBack] = {
        equals: this.pkey,
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
    },
  },
};
</script>

<docs>
Example
note, this input doesn't have value on its own, it just allows you to edit the refback in context.
This also means you cannot do this unless your current record has a pkey to point to
```
<InputRefback
    label="Contributions"
    table="Contributions"
    refBack="resource"
    :refbackValue="{acronym:'ALSPAC'}"
    graphqlURL="/CohortNetwork/graphql"
/>

```
</docs>
