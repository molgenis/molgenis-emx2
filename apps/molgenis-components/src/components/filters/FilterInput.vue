<template>
<div>
    <component
      :is="filterType"
      :id="id"
      :name="name"
      :condition="conditions[0]"
      @updateCondition="updateCondition(0, $event)"
    ></component>

    <div v-for="(condition, index) in conditions.slice(1)" :key="index + 1">
      <component
        :is="filterType"
        :id="id + index + 1"
        :name="name"
        :condition="condition[index + 1]"
        @updateCondition="updateCondition(index + 1, $event)"
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
  computed: {
    filterType() {
      return StringFilter;
    },
  },
  methods: {
    updateCondition(index, value) {
      let updatedConditions= [...this.conditions]
      if(!this.conditions.length) {
        updatedConditions = [value]
      } else {
        updatedConditions[index] = value
      }
      this.$emit("updateConditions", updatedConditions)
    }
  },
};
</script>

<style>
</style>