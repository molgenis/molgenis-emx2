<template>
  <input
    v-if="rowKey"
    :id="`${id}-${rowAsString}`"
    :name="`${id}-${rowAsString}`"
    type="radio"
    :checked="isSelected"
    @change="$emit('update:modelValue', rowKey)"
    class="form-check-input"
    :class="{ 'is-invalid': errorMessage }"
  />
  <label class="form-check-label" :for="`${id}-${rowAsString}`">
    {{ rowAsString }}
  </label>
</template>

<script lang="ts">
import BaseInput from "./baseInputs/BaseInput.vue";
import { flattenObject } from "../utils";
import { IRow } from "../../Interfaces/IRow";

export default {
  name: "InputRefItem",
  extends: BaseInput,
  props: {
    id: { required: true, type: String },
    tableName: { required: true, type: String },
    row: { required: true, type: Object },
    selection: { required: false, type: Object },
    client: { required: true, type: Object },
  },
  data() {
    return {
      rowKey: "",
    };
  },
  computed: {
    rowAsString() {
      return flattenObject(this.rowKey).trim();
    },
    isSelected() {
      return this.rowKey?.name === this.selection?.name;
    },
  },
  mounted() {
    this.client
      .convertRowToPrimaryKey(this.row, this.tableName)
      .then((rowKey: IRow) => {
        this.rowKey = rowKey;
      });
  },
};
</script>
