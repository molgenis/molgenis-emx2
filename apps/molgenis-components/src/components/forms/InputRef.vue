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
        <ButtonAlt
          v-if="modelValue !== null"
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
          <InputRefItem
            :id="id"
            :row="row"
            :tableName="tableName"
            :client="client"
            :selection="modelValue"
            :errorMessage="errorMessage"
            @update:modelValue="select"
          ></InputRefItem>
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
            :selection="[modelValue]"
            :lookupTableName="tableName"
            :filter="filter"
            @select="select($event)"
            @deselect="clearValue"
            @close="loadOptions"
            :schemaName="schemaName"
            :showSelect="true"
            :limit="10"
            :canEdit="canEdit"
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
import Client from "../../client/client.ts";
import BaseInput from "./baseInputs/BaseInput.vue";
import TableSearch from "../tables/TableSearch.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import FormGroup from "./FormGroup.vue";
import ButtonAlt from "./ButtonAlt.vue";
import InputRefItem from "./InputRefItem.vue";
import { flattenObject, convertToPascalCase } from "../utils";

export default {
  name: "InputRef",
  extends: BaseInput,
  components: {
    TableSearch,
    LayoutModal,
    FormGroup,
    ButtonAlt,
    InputRefItem,
  },
  props: {
    schemaName: {
      required: false,
      type: String,
    },
    filter: Object,
    multipleColumns: Boolean,
    itemsPerColumn: { type: Number, default: 12 },
    maxNum: { type: Number, default: 11 },
    tableName: {
      type: String,
      required: true,
    },
    /**
     * Whether or not the buttons are show to edit the referenced table
     *  */
    canEdit: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  data: function () {
    return {
      client: null,
      showSelect: false,
      data: [],
      count: 0,
    };
  },
  computed: {
    tableId() {
      return convertToPascalCase(this.tableName);
    },
    title() {
      return "Select " + this.tableName;
    },
    showMultipleColumns() {
      return this.multipleColumns && this.count > this.itemsPerColumn;
    },
  },
  methods: {
    clearValue() {
      this.$emit("update:modelValue", null);
    },
    select(event) {
      this.$emit("update:modelValue", event);
    },
    openSelect() {
      this.showSelect = true;
    },
    closeSelect() {
      this.loadOptions();
      this.showSelect = false;
    },
    flattenObject,
    async loadOptions() {
      const options = {
        limit: this.maxNum,
      };
      if (this.filter) {
        options.filter = this.filter;
      }
      const response = await this.client.fetchTableData(this.tableId, options);
      this.data = response[this.tableId];
      this.count = response[this.tableId + "_agg"].count;
    },
  },
  watch: {
    filter() {
      this.loadOptions();
    },
  },
  async created() {
    this.client = Client.newClient(this.schemaName);
    await this.loadOptions();
  },
};
</script>

<docs>
<template>
  <div>
    You have to be have server running and be signed in for this to work
     <div class="border-bottom mb-3 p-2">
       <h5>synced demo props: </h5>
         <div>
           <label for="canEdit" class="pr-1">can edit: </label>
           <input type="checkbox" id="canEdit" v-model="canEdit">
         </div>
         <p class="font-italic">view in table mode to see edit action buttons</p>
    </div>
    <DemoItem>
      <!-- normally you don't need schemaName, usually scope is all you need -->
      <InputRef
        id="input-ref"
        label="Standard ref input"
        v-model="value"
        tableName="Pet"
        description="Standard input"
        schemaName="pet store"
        :canEdit="canEdit"
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
        schemaName="pet store"
        :canEdit="canEdit"
      />
      Selection: {{ defaultValue }}
    </DemoItem>
    <DemoItem>
      <InputRef
        id="input-ref-filter"
        label="Ref input with pre set filter ( only cats)"
        v-model="filterValue"
        tableName="Pet"
        description="Filter by name"
        :filter="{ category: { name: { equals: 'cat' } } }"
        schemaName="pet store"
        :canEdit="canEdit"
      />
      Selection: {{ filterValue }}
      <br />
      Filter: { category: { name: { equals: 'cat' } } }
    </DemoItem>
    <DemoItem>
      <InputRef
        id="input-ref-multi-column"
        label="Ref input with multiple columns"
        v-model="multiColumnValue"
        tableName="Pet"
        description="This is a multi column input"
        schemaName="pet store"
        multipleColumns
        :itemsPerColumn="3"
        :canEdit="canEdit"
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
      defaultValue: { name: "spike" },
      filterValue: { name: "spike" },
      multiColumnValue: null,
      canEdit: false
    };
  },
};
</script>
</docs>
