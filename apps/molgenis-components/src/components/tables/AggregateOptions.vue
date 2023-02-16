<template>
  <div class="aggregate-options container" v-if="refColumns.length">
    <div class="row">
      <div class="col-1">
        <label class="mr-2 col-form-label" for="aggregate-column-select">
          Column:
        </label>
      </div>
      <div class="col-3">
        <InputSelect
          id="aggregate-column-select"
          class="column-select"
          required
          :modelValue="selectedColumn"
          @update:modelValue="$emit('update:selectedColumn', $event)"
          :options="refColumns"
        />
      </div>
    </div>
    <div class="row">
      <div class="col-1">
        <label class="mr-2 col-form-label" for="aggregate-row-select">
          Row:
        </label>
      </div>
      <div class="col-3">
        <InputSelect
          id="aggregate-row-select"
          class="row-select"
          required
          :modelValue="selectedRow"
          @update:modelValue="$emit('update:selectedRow', $event)"
          :options="refColumns"
        />
      </div>
    </div>
  </div>
</template>

<style>
.aggregate-options .float-right {
  display: none;
}
</style>

<script lang="ts">
import { defineComponent } from "vue";
import { IColumn } from "../../Interfaces/IColumn";
import InputSelect from "../forms/InputSelect.vue";

export default defineComponent({
  name: "AggregateOptions",
  components: { InputSelect },
  props: {
    columns: {
      type: Array,
      required: true,
    },
    selectedColumn: {
      type: String,
      required: true,
    },
    selectedRow: {
      type: String,
      required: true,
    },
  },
  data: function () {
    return {
      loading: true,
      refColumns: [] as string[],
    };
  },
  methods: {
    isRefType(column: IColumn): boolean {
      return (
        column.columnType.startsWith("REF") ||
        column.columnType.startsWith("ONTOLOGY")
      );
    },
    getRefTypeColumns(columns: IColumn[]): string[] {
      return columns
        .filter((column: IColumn) => this.isRefType(column))
        .map((column: IColumn) => column.name);
    },
  },
  created() {
    if (this.columns.length > 0) {
      this.refColumns = this.getRefTypeColumns(this.columns as IColumn[]);
      if (this.refColumns?.length > 0) {
        this.$emit("setAggregateColumns", this.refColumns);
        this.$emit("update:selectedColumn", this.refColumns[0]);
        this.$emit(
          "update:selectedRow",
          this.refColumns[1] || this.refColumns[0]
        );
        this.loading = false;
      }
    }
  },
});
</script>

<docs>
<template>
  <demo-item>
    <AggregateOptions
        :columns="columns"
        selectedColumn="category"
        selectedRow="tags"
    />
  </demo-item>
</template>

<script>
  export default {
    data() {
      return {
        columns: [{name: 'category', columnType: 'REF', required: true}, {name: 'tags', columnType: 'REF', required: true} ]
      };
    },
  };
</script>
</docs>
