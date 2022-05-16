<template>
  <div>
    <div v-for="index in fieldCount" :key="index">
      <component
        :is="filterType"
        :id="id + index"
        :condition="conditions[index - 1]"
        @updateCondition="updateCondition(index - 1, $event)"
        @clearCondition="clearCondition(index - 1)"
        @addCondition="fieldCount++"
        :showAddButton="index === conditions.length"
      ></component>
    </div>
  </div>
</template>

<script>
import StringFilter from "./StringFilter.vue";
import IntegerFilter from "./IntegerFilter.vue";
import DecimalFilter from "./DecimalFilter.vue";
import DateFilter from "./DateFilter.vue";

const filterTypeMap = {
  STRING: StringFilter,
  STRING_ARRAY: StringFilter,
  TEXT: StringFilter,
  TEXT_ARRAY: StringFilter,
  UUID: StringFilter,
  UUID_ARRAY: StringFilter,
  INT: IntegerFilter,
  INT_ARRAY: IntegerFilter,
  DECIMAL: DecimalFilter,
  DECIMAL_ARRAY: DecimalFilter,
  DATE: DateFilter,
  DATE_ARRAY: DateFilter,
};

export default {
  name: "FilterInput",
  components: { StringFilter, IntegerFilter, DecimalFilter, DateFilter },
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
    name: {
      type: String,
      required: false,
      default: function () {
        return this.id;
      },
    },
  },
  data() {
    return {
      // used to add new empty field when adding conditions
      fieldCount: this.conditions.length || 1,
    };
  },
  computed: {
    filterType() {
      return filterTypeMap[this.columnType];
    },
  },
  methods: {
    updateCondition(index, value) {
      let updatedConditions = [...this.conditions];
      if (!this.conditions.length) {
        updatedConditions = [value];
      } else {
        updatedConditions[index] = value;
      }
      this.$emit("updateConditions", updatedConditions);
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
      this.fieldCount = newValue.length || 1;
    },
  },
};
</script>

<style></style>

<docs>
<template>
  <div>
    <div>
      <label>empty string filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-example-1"
            columnType="STRING"
            :conditions="conditions"
            @updateConditions="conditions = $event"
        />
        <div>conditions: {{ conditions }}</div>
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
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        conditions: [],
        conditions1: ["tst"],
        conditions2: [[1, 3]],
        conditions3: []
      };
    },
  };
</script>
</docs>
