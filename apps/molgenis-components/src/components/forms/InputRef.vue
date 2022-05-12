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
          v-for="(row, index) in notSelectedRows"
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
            count > limit ? "view all " + count + " options." : "view as table"
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

export default {
  extends: BaseInput,
  data: function () {
    return {
      showSelect: false,
      selectIdx: null,
      options: [],
      selection: [],
      data: null,
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
    /** change if graphql URL != 'graphql'*/
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
    notSelectedRows() {
      if (this.data) {
        let result = this.data.filter(
          (row) =>
            this.count <= this.maxNum ||
            !this.selection.some(
              (v) =>
                this.flattenObject(
                  this.getPrimaryKey(row, this.tableMetaData)
                ) == this.flattenObject(v)
            )
        );
        //truncate on maxNum (we overquery because we include all selected which might not be in query)
        result.length = Math.min(result.length, this.maxNum);
        return result;
      }
      return [];
    },
    showMultipleColumns() {
      return this.multipleColumns && this.count > 12;
    },
  },
  methods: {
    getPrimaryKey(row, tableMetadata) {
      //we only have pkey when the record has been saved
      if (!row["mg_insertedOn"]) {
        return null;
      }

      let result = {};
      if (tableMetadata !== null) {
        tableMetadata.columns.forEach((col) => {
          if (col.key == 1 && row[col.id]) {
            result[col.id] = row[col.id];
          }
        });
      }
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
      let result = "";
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += " " + object[key];
        }
      });
      return result;
    },
  },
  watch: {
    value() {
      this.selection = this.value ? this.value : [];
      //we overquery because we include all selected which might not be in query
      this.limit = this.maxNum + this.selection.length;
    },
  },
  created() {
    this.limit = this.maxNum + this.selection.length;
    this.selection = this.value ? this.value : [];
  },
  async mounted() {
    const client = Client.newClient(this.graphqlURL);
    this.tableMetaData = (await client.fetchMetaData()).tables.find(
      (table) => table.id === this.tableName
    );
    this.data = (await client.fetchTableData(this.tableName))[this.tableName];
    this.count = this.data.length; // ?
    console.log(this.data);
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
        v-model="defaultValue"
        tableName="Pet"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ JSON.stringify(defaultValue) }}
    </DemoItem>
    <!--
    <DemoItem>
      <InputRef
        id="input-ref-default"
        label="My pets"
        v-model="defaultValue"
        tableName="Pet"
        :defaultValue="defaultValue"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ defaultValue }}
    </DemoItem>
    <DemoItem>
      <InputRef
        id="input-ref-filter"
        v-model="filterValue"
        tableName="Pet"
        :filter="{ category: { name: { equals: 'dog' } } }"
        graphqlURL="/pet store/graphql"
      />
      Selection: {{ filterValue }}
    </DemoItem>
    -->
  </div>
</template>

<script>
  export default {
    data: function () {
      return {
        value: null,
        defaultValue: {name: 'spike'},
        filterValue: {name: 'spike'}
      };
    }
  };
</script>
</docs>
