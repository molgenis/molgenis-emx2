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

export default {
  name: "FilterInput",
  components: { StringFilter },
  props: {
    id: {
      type: String,
      required: true,
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
      return StringFilter;
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
            :conditions="conditions"
            @updateConditions="conditions = $event"
        ></FilterInput>
        <div>conditions: {{ conditions }}</div>
      </demo-item>
    </div>

    <div class="mt-3">
      <label>pre-filled string filter</label>
      <demo-item>
        <FilterInput
            id="filter-input-example-2"
            :conditions="conditions1"
            @updateConditions="conditions1 = $event"
        ></FilterInput>
        <div>conditions: {{ conditions1 }}</div>
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
      };
    },
  };
</script>
</docs>
