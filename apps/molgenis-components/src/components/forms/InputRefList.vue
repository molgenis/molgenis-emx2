<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <div>
      <div>
        <div v-if="count > maxNum">
          <FilterWell
            v-for="(item, key) in selection"
            :key="JSON.stringify(item)"
            :label="flattenObject(item)"
            @click="deselect(key)"
          />
        </div>
        <ButtonAlt
          v-if="value && value.length"
          class="pl-1"
          @click="clearValue"
        >
          clear selection
        </ButtonAlt>
      </div>
      <div
        :class="
          showMultipleColumns ? 'd-flex align-content-stretch flex-wrap' : ''
        "
      >
        <div
          class="form-check custom-control custom-checkbox"
          :class="showMultipleColumns ? 'col-12 col-md-6 col-lg-4' : ''"
          v-for="(row, index) in data"
          :key="index"
        >
          <input
            :id="`${id}-${row.name}`"
            :name="id"
            type="checkbox"
            :value="getPrimaryKey(row, tableMetaData)"
            v-model="selection"
            @change="emitSelection"
            class="form-check-input"
            :class="{ 'is-invalid': errorMessage }"
          />
          <label class="form-check-label" :for="`${id}-${row.name}`">
            {{ flattenObject(getPrimaryKey(row, tableMetaData)) }}
          </label>
        </div>
        <ButtonAlt
          class="pl-0"
          :class="showMultipleColumns ? 'col-12 col-md-6 col-lg-4' : ''"
          icon="fa fa-search"
          @click="openSelect"
        >
          {{ count > maxNum ? `view all ${count} options.` : "view as table" }}
        </ButtonAlt>
      </div>
      <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
        <template v-slot:body>
          <TableSearch
            :selection.sync="selection"
            @update:selection="$emit('input', $event)"
            :lookupTableName="tableName"
            :filter="filter"
            @select="emitSelection"
            @deselect="deselect"
            :graphqlURL="graphqlURL"
            :showSelect="true"
            :limit="10"
          />
        </template>
        <template v-slot:footer>
          <ButtonAlt @click="closeSelect">Close</ButtonAlt>
        </template>
      </LayoutModal>
    </div>
  </FormGroup>
</template>

<script>
import Client from "../../client/client.js";
import BaseInput from "./baseInputs/BaseInput.vue";
import TableSearch from "../tables/TableSearch.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import FormGroup from "./FormGroup.vue";
import ButtonAlt from "./ButtonAlt.vue";
import FilterWell from "../filters/FilterWell"
import { flattenObject, getPrimaryKey } from "../utils";

export default {
  extends: BaseInput,
  data: function () {
    return {
      showSelect: false,
      data: [],
      selection: this.value,
      count: 0,
      tableMetaData: null,
    };
  },
  components: {
    FilterWell,
    TableSearch,
    LayoutModal,
    FormGroup,
    ButtonAlt,
  },
  props: {
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    filter: Object,
    multipleColumns: Boolean,
    maxNum: { type: Number, default: 11 },
    tableName: {
      type: String,
      required: true,
    },
  },
  computed: {
    title() {
      return "Select " + this.tableName;
    },
    showMultipleColumns() {
      const itemsPerColumn = 12;
      return this.multipleColumns && this.count > itemsPerColumn;
    },
  },
  methods: {
    getPrimaryKey,
    deselect(key) {
      this.selection.splice(key, 1);
      this.emitSelection();
    },
    clearValue() {
      this.selection = [];
      this.emitSelection();
    },
    emitSelection() {
      this.$emit("input", this.selection);
    },
    openSelect() {
      this.showSelect = true;
    },
    closeSelect() {
      this.showSelect = false;
    },
    flattenObject,
  },
  async mounted() {
    const client = Client.newClient(this.graphqlURL);
    const allMetaData = await client.fetchMetaData();
    this.tableMetaData = allMetaData.tables.find(
      (table) => table.id === this.tableName
    );

    const options = {
      limit: this.maxNum,
    };
    const response = await client.fetchTableData(this.tableName, options);
    this.data = response[this.tableName];
    this.count = response[this.tableName + "_agg"].count;

    if (!this.value) {
      this.selection = [];
    }
  },
};
</script>

<docs>
<template>
  <div>
    You have to be have server running and be signed in for this to work
    <DemoItem>
      <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
      <InputRefList
        id="input-ref-list"
        label="Standard ref input list"
        v-model="value"
        tableName="Pet"
        description="Standard input"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ value }}
    </DemoItem>
    <DemoItem>
      <InputRefList
        id="input-ref-list-default"
        label="Ref input list with default value"
        v-model="defaultValue"
        tableName="Pet"
        description="This is a default value"
        :defaultValue="defaultValue"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ defaultValue }}
    </DemoItem>
    <DemoItem>
      <InputRefList
        id="input-ref-list-filter"
        label="Ref input list with pre set filter"
        v-model="filterValue"
        tableName="Pet"
        description="Filter by name"
        :filter="{ category: { name: { equals: 'pooky' } } }"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ filterValue }}
    </DemoItem>
    <DemoItem>
      <InputRefList
        id="input-ref-list"
        label="Ref input list with multiple columns"
        v-model="multiColumnValue"
        tableName="Pet"
        description="This is a multi column input"
        graphqlURL="/pet store/graphql"
        multipleColumns
      />
      Selection: {{ multiColumnValue }}
    </DemoItem>
  </div>
</template>

<script>
export default {
  data: function () {
    return {
      value: null,
      defaultValue: [{ name: "pooky" }, { name: "spike" }],
      filterValue: [{ name: "spike" }],
      multiColumnValue: null,
    };
  },
};
</script>
</docs>
