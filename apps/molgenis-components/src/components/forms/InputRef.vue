<template>
  <FormGroup :id="id" :label="label" :description="description">
    <div>
      <div>
        <ButtonAlt
          v-if="value !== null"
          class="pl-1"
          icon="fa fa-clear"
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
            type="radio"
            :value="getPrimaryKey(row, tableMetaData)"
            :checked="isSelected(row)"
            @change="$emit('input', getPrimaryKey(row, tableMetaData))"
            class="form-check-input"
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
            :selection="[value]"
            :lookupTableName="tableName"
            :filter="filter"
            @select="select($event)"
            @deselect="clearValue"
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
import { flattenObject, getPrimaryKey } from "../utils";

export default {
  name: "InputRef",
  extends: BaseInput,
  data: function () {
    return {
      showSelect: false,
      data: [],
      count: 0,
      tableMetaData: null,
    };
  },
  components: {
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
    clearValue() {
      this.$emit("input", null);
    },
    select(event) {
      this.$emit("input", event);
    },
    openSelect() {
      this.showSelect = true;
    },
    closeSelect() {
      this.showSelect = false;
    },
    isSelected(row) {
      return (
        this.getPrimaryKey(row, this.tableMetaData)?.name ===
        (this.value ? this.value.name : "")
      );
    },
    flattenObject,
  },
  async mounted() {
    const client = Client.newClient(this.graphqlURL);
    this.tableMetaData = (await client.fetchMetaData()).tables.find(
      (table) => table.id === this.tableName
    );
    const options = {
      limit: this.maxNum,
    };
    const response = await client.fetchTableData(this.tableName, options);
    this.data = response[this.tableName];
    this.count = response[this.tableName + "_agg"].count;
  },
};
</script>

<docs>
<template>
  <div>
    You have to be have server running and be signed in for this to work
    <DemoItem>
      <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
      <InputRef
        id="input-ref"
        label="Standard ref input"
        v-model="value"
        tableName="Pet"
        description="Standard input"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ value }}
    </DemoItem>
    <DemoItem>
      <InputRef
        id="input-ref-default"
        label="Ref with default value"
        v-model="defaultValue"
        tableName="Pet"
        description="This is a default value"
        :defaultValue="defaultValue"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ defaultValue }}
    </DemoItem>
    <DemoItem>
      <InputRef
        id="input-ref-filter"
        label="Ref input with pre set filter"
        v-model="filterValue"
        tableName="Pet"
        description="Filter by name"
        :filter="{ category: { name: { equals: 'pooky' } } }"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ filterValue }}
    </DemoItem>
    <DemoItem>
      <InputRef
        id="input-ref"
        label="Ref input with multiple columns"
        v-model="multiColumnValue"
        tableName="Pet"
        description="This is a multi column input"
        graphqlURL="/pet store/graphql"
        multipleColumns
      />
      Selection: {{ value }}
    </DemoItem>
  </div>
</template>

<script>
export default {
  data: function () {
    return {
      value: null,
      defaultValue: { name: "spike" },
      filterValue: { name: "spike" },
      multiColumnValue: null,
    };
  },
};
</script>
</docs>
