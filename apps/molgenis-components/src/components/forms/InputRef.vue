<template>
  <FormGroup :id="id" :label="label" :description="description">
    <div>
      <div>
        <ButtonAlt
          v-if="selection.length > 0"
          class="pl-1"
          icon="fa fa-clear"
          @click="emitClear"
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
            class="form-check-input"
            :id="id + index"
            :name="id"
            type="radio"
            :value="getPrimaryKey(row, tableMetaData)"
            v-model="selection"
            @change="$emit('input', getPrimaryKey(row, tableMetaData))"
          />
          <label class="form-check-label" :for="id + index">
            {{ flattenObject(getPrimaryKey(row, tableMetaData)) }}
          </label>
        </div>
        <ButtonAlt
          class="pl-0"
          :class="showMultipleColumns ? 'col-12 col-md-6 col-lg-4' : ''"
          icon="fa fa-search"
          @click="showSelect = true"
        >
          {{
            count > maxNum ? "view all " + count + " options." : "view as table"
          }}
        </ButtonAlt>
      </div>
      <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
        <template v-slot:body>
          <TableSearch
            :selection="[value]"
            :lookupTableName="tableName"
            :filter="filter"
            @select="select($event)"
            @deselect="emitClear"
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
import BaseInput from "./BaseInput.vue";
import TableSearch from "../tables/TableSearch.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import FormGroup from "./FormGroup.vue";
import ButtonAlt from "./ButtonAlt.vue";
import _ from "lodash";

export default {
  extends: BaseInput,
  data: function () {
    return {
      showSelect: false,
      selectIdx: null,
      options: [],
      selection: [],
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
      return "Select " + this.table;
    },
    showMultipleColumns() {
      const itemsPerColumn = 12;
      return this.multipleColumns && this.count > itemsPerColumn;
    },
  },
  methods: {
    getPrimaryKey(row, tableMetadata) {
      //we only have pkey when the record has been saved
      if (!row["mg_insertedOn"]) {
        return null;
      }
      let result = {};
      tableMetadata?.columns.forEach((column) => {
        if (column.key === 1 && row[column.id]) {
          result[column.id] = row[column.id];
        }
      });
      return result;
    },
    deselect(key) {
      this.selection.splice(key, 1);
      this.$emit("input", this.selection);
    },
    emitClear() {
      this.$emit("input", null);
    },
    select(event) {
      this.$emit("input", event);
    },
    closeSelect() {
      this.showSelect = false;
    },
    openSelect(idx) {
      this.showSelect = true;
      this.selectIdx = idx;
    },
    flattenObject(object) {
      if (typeof object === "object") {
        return _.reduce(
          object,
          (accum, value) => {
            if (value === null) {
              return accum;
            }
            if (typeof value === "object") {
              accum += this.flattenObject(value);
            } else {
              accum += " " + value;
            }
            return accum;
          },
          ""
        );
      } else {
        return object;
      }
    },
  },
  watch: {
    value() {
      this.selection = this.value ? this.value : [];
    },
    selection() {
      console.log(JSON.stringify(this.selection));
    },
  },
  created() {
    this.selection = this.value ? this.value : [];
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
      <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
      <InputRef
        id="input-ref"
        label="Ref input with multiple columns"
        v-model="multiColValue"
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
        defaultValue: {name: 'spike'},
        filterValue: {name: 'spike'},
        multiColValue: null,	    
      };
    }
  };
</script>
</docs>
