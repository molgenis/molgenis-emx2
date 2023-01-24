<template>
  <div class="btn-group aggregate-options">
    <span>
      <InputSelect
        id="aggregate-column-select"
        class="column-select"
        required
        :modelValue="selectedColumnHeader"
        @update:modelValue="$emit('update:selectedColumnHeader', $event)"
        :options="refColumns"
      />
    </span>
    <span>
      <InputSelect
        id="aggregate-row-select"
        class="row-select"
        required
        :modelValue="selectedRowHeader"
        @update:modelValue="$emit('update:selectedRowHeader', $event)"
        :options="refColumns"
      />
    </span>
  </div>
</template>

<style>
.aggregate-options .float-right {
  display: none;
}

.column-select select {
  border-color: var(--primary);
  border-right: 0px;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  padding-right: 1rem;
  color: var(--primary);
}

.row-select select {
  border-color: var(--primary);
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  color: var(--primary);
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
    selectedColumnHeader: {
      type: String,
      required: true,
    },
    selectedRowHeader: {
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
        this.$emit("update:selectedColumnHeader", this.refColumns[0]);
        this.$emit(
          "update:selectedRowHeader",
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
        selectedColumnHeader="category"
        selectedRowHeader="tags"
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
