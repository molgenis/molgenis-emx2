<template>
  <div>
    <div v-if="isMultiConditionFilter">
      <component
        :is="filterType"
        :id="id"
        :condition="conditions"
        @updateCondition="updateCondition(index - 1, $event)"
        :tableId="tableId"
        :schemaId="schemaId"
        :refLabel="refLabel"
      ></component>
    </div>
    <div v-else v-for="index in fieldCount" :key="index">
      <component
        :is="filterType"
        :id="id + index"
        :condition="conditions[index - 1]"
        @updateCondition="updateCondition(index - 1, $event)"
        @clearCondition="clearCondition(index - 1)"
        @addCondition="fieldCount++"
        :showAddButton="index === conditions.length"
        :tableId="tableId"
        :schemaId="schemaId"
      ></component>
    </div>
  </div>
</template>

<script>
import StringFilter from "./StringFilter.vue";
import IntegerFilter from "./IntegerFilter.vue";
import DecimalFilter from "./DecimalFilter.vue";
import DateFilter from "./DateFilter.vue";
import DateTimeFilter from "./DateTimeFilter.vue";
import BooleanFilter from "./BooleanFilter.vue";
import RefListFilter from "./RefListFilter.vue";
import OntologyFilter from "./OntologyFilter.vue";
import LongFilter from "./LongFilter.vue";
import { deepClone } from "../utils.ts";

const filterTypeMap = {
  STRING: StringFilter,
  STRING_ARRAY: StringFilter,
  EMAIL: StringFilter,
  EMAIL_ARRAY: StringFilter,
  HYPERLINK: StringFilter,
  HYPERLINK_ARRAY: StringFilter,
  TEXT: StringFilter,
  TEXT_ARRAY: StringFilter,
  UUID: StringFilter,
  UUID_ARRAY: StringFilter,
  INT: IntegerFilter,
  INT_ARRAY: IntegerFilter,
  DECIMAL: DecimalFilter,
  DECIMAL_ARRAY: DecimalFilter,
  LONG: LongFilter,
  LONG_ARRAY: LongFilter,
  DATE: DateFilter,
  DATE_ARRAY: DateFilter,
  DATETIME: DateTimeFilter,
  DATETIME_ARRAY: DateTimeFilter,
  BOOL: BooleanFilter,
  BOOl_ARRAY: BooleanFilter,
  REF: RefListFilter,
  REFBACK: RefListFilter,
  REF_ARRAY: RefListFilter,
  ONTOLOGY: OntologyFilter,
  ONTOLOGY_ARRAY: OntologyFilter,
};

export default {
  name: "FilterInput",
  components: {
    StringFilter,
    IntegerFilter,
    DecimalFilter,
    DateFilter,
    DateTimeFilter,
    BooleanFilter,
    RefListFilter,
    OntologyFilter,
  },
  props: {
    id: {
      type: String,
      required: true,
    },
    columnType: {
      type: String,
      required: true,
      validator(value) {
        return Object.keys(filterTypeMap).includes(value);
      },
    },
    conditions: {
      type: Array,
      required: true,
    },
    tableId: {
      type: String,
      required: false,
    },
    schemaId: {
      type: String,
      required: false,
    },
    refLabel: {
      type: String,
      required: false,
    },
  },
  data() {
    return {
      // used to add new empty field when adding conditions
      fieldCount: this.isMultiConditionFilter ? 1 : this.conditions.length || 1,
    };
  },
  computed: {
    filterType() {
      return filterTypeMap[this.columnType];
    },
    isMultiConditionFilter() {
      return [
        "REF",
        "REF_ARRAY",
        "REFBACK",
        "ONTOLOGY",
        "ONTOLOGY_ARRAY",
      ].includes(this.columnType);
    },
  },
  methods: {
    updateCondition(index, value) {
      if (this.isMultiConditionFilter) {
        this.$emit("updateConditions", deepClone(value));
      } else if (!this.conditions.length) {
        this.$emit("updateConditions", [value]);
      } else {
        let updatedConditions = deepClone(this.conditions);
        updatedConditions[index] = value;
        this.$emit("updateConditions", updatedConditions);
      }
    },
    clearCondition(index) {
      let updatedConditions = [...this.conditions];
      updatedConditions.splice(index, 1);
      this.$emit("updateConditions", updatedConditions);
      if (this.fieldCount > 1) {
        this.fieldCount--;
      }
    },
  },
  watch: {
    conditions(newValue) {
      this.fieldCount = this.isMultiConditionFilter ? 1 : newValue.length || 1;
    },
  },
  emits: ["updateConditions"],
};
</script>

<docs>
<template>
  <div>
    <div>
      <label>empty string filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-string1"
            columnType="STRING"
            :conditions="conditions"
            @updateConditions="conditions = $event"
        />
        <div>conditions: {{ conditions }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>pre-filled string filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-string2"
            columnType="STRING"
            :conditions="conditions1"
            @updateConditions="conditions1 = $event"
        />
        <div>conditions: {{ conditions1 }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>pre-filled int filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-int"
            columnType="INT"
            :conditions="conditions2"
            @updateConditions="conditions2 = $event"
        />
        <div>conditions: {{ conditions2 }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>date filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-date"
            columnType="DATE"
            :conditions="conditions3"
            @updateConditions="conditions3 = $event"
        />
        <div>conditions: {{ conditions3 }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>decimal filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-decimal"
            columnType="DECIMAL"
            :conditions="conditions4"
            @updateConditions="conditions4 = $event"
        />
        <div>conditions: {{ conditions4 }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>boolean filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-boolean"
            columnType="BOOL"
            :conditions="conditions5"
            @updateConditions="conditions5 = $event"
        />
        <div>conditions: {{ conditions5 }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>ontology filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-ontology"
            columnType="ONTOLOGY"
            tableId="Tag"
            schemaId="pet store"
            :conditions="conditions6"
            @updateConditions="conditions6 = $event"
        />
        <div>conditions: {{ conditions6 }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>ref filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-ref"
            columnType="REF"
            tableId="Tag"
            schemaId="pet store"
            :conditions="conditions7"
            @updateConditions="conditions7 = $event"
            refLabel="${name}"
        />
        <div>conditions: {{ conditions7 }}</div>
      </demo-item>
    </div>
    <div class="mt-3">
      <label>ref_array filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-reflist"
            columnType="REF_ARRAY"
            tableId="Tag"
            schemaId="pet store"
            :conditions="conditions8"
            @updateConditions="conditions8 = $event"
            refLabel="${name}"
        />
        <div>conditions: {{ conditions8 }}</div>
      </demo-item>
    </div>
  </div>
</template>
<script>
  export default {
    data () {
      return {
        conditions: [],
        conditions1: ["tst"],
        conditions2: [[1, 3]],
        conditions3: [],
        conditions4: [],
        conditions5: [],
        conditions6: [],
        conditions7: [],
        conditions8: []
      };
    },
  };
</script>
</docs>
