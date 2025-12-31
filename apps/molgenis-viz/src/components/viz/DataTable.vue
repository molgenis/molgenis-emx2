<template>
  <table :id="tableId" :class="tableClassNames">
    <caption v-if="caption">
      {{
        caption
      }}
    </caption>
    <thead role="">
      <tr>
        <th
          v-for="(column, index) in columnOrder"
          role="columnheader"
          scope="col"
          :data-column-index="index"
          :data-column-name="column"
          :key="column"
          :class="numericColumns.includes(column) ? 'column-numeric' : ''"
        >
          {{ column }}
        </th>
      </tr>
    </thead>
    <tbody role="presentation">
      <tr
        v-for="(row, rowindex) in data"
        :key="rowindex"
        :data-row-index="rowindex"
        @click="onClick(row)"
      >
        <td
          v-for="(column, colindex) in columnOrder"
          role="gridcell"
          :data-column-index="colindex"
          :data-column-name="column"
          :data-cell-index="`${rowindex},${colindex}`"
          :class="dataTypeToCssClass(column, row[column])"
        >
          <span class="cell-colname" aria-hidden="true">{{ column }}</span>
          <span
            class="cell-value"
            v-if="renderHtml"
            v-html="row[column]"
          ></span>
          <span class="cell-value" v-else>{{ row[column] }}</span>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script>
// @displayName Datatable
// The datatable component renders a dataset into a responsive, interactive table. By default, all tables are rendered with interactive features enabled (i.e., row highlighting and row clicks), but these can be disabled as needed. Column selection and order can be defined using the `columnOrder` property. This allows you to customise the layout of the table rather than processing the data beforehand.
//
// @group VISUALISATIONS
export default {
  name: "DataTable",
  props: {
    // A unique identifier for the table
    tableId: {
      type: String,
      required: true,
    },
    // dataset to render (array of objects)
    data: {
      type: Array,
      required: true,
    },
    // an array of column names that define the selection and order of columns
    columnOrder: {
      type: Array,
      required: true,
    },
    // optional text that describes the table
    caption: {
      type: String,
      default: null,
    },
    // If true, rows will be highlighted on mouse events
    enableRowHighlighting: {
      type: Boolean,
      default: true,
    },
    // If true, row clicks will return the selected row (as an object)
    // Row level data can be access using the following event
    // `@row-clicked=...`
    enableRowClicks: {
      type: Boolean,
      default: true,
    },
    // If true, all values will be rendered as HTML. Otherwise, values will be rendered as text
    renderHtml: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["row-clicked"],
  computed: {
    tableClassNames() {
      const base = "d3-viz d3-table";
      const highlighting = this.enableRowHighlighting
        ? "table-row-highlighting"
        : "";
      return [base, highlighting].join(" ");
    },
  },
  data() {
    return {
      numericColumns: [],
    };
  },
  methods: {
    dataTypeToCssClass(column, value) {
      let css = `column-${column} data-value`;
      const type = typeof value;
      css += ` value-${type}`;
      if (type === "number") {
        if (!this.numericColumns.includes(column)) {
          this.numericColumns.push(column);
        }
        if (value > 0) {
          css += " value-positive";
        } else if (value < 0) {
          css += " value-negative";
        } else {
          css += " value-zero";
        }
      }
      return css;
    },
    onClick(data) {
      this.$emit("row-clicked", data);
    },
  },
};
</script>

<style lang="scss">
@mixin visuallyHidden {
  position: absolute;
  clip: rect(1px 1px 1px 1px);
  clip: rect(1px, 1px, 1px, 1px);
  width: 1px;
  height: 1px;
  overflow: hidden;
  white-space: nowrap;
}

@mixin revealHiddenContent {
  position: static;
  display: block;
  clip: auto;
  height: auto;
  width: auto;
  overflow: visible;
  white-space: normal;
}

@mixin columnHeader {
  font-size: 11pt;
  font-weight: 600;
  padding: 4px 12px;
  text-transform: uppercase;
  letter-spacing: 2px;
}

.d3-table {
  border-spacing: 0;
  text-align: left;
  color: $gray-700;
  width: 100%;
  position: relative;

  caption {
    caption-side: top;
    text-align: left;
    font-size: inherit;
    margin-bottom: 16px;
    color: $gray-900;
  }

  thead {
    tr {
      th {
        @include columnHeader;
        border-bottom: 2px solid $gray-900;
        color: $gray-900;

        &.column-numeric {
          text-align: right;
        }
      }
    }
  }

  tbody {
    tr {
      td {
        font-size: 13pt;
        padding: 16px 12px;

        &.value-number {
          text-align: right;
        }

        .cell-colname {
          @include visuallyHidden;
        }
      }

      &:nth-child(even) {
        background-color: $gray-050;
      }

      &:last-child {
        td {
          border-bottom: 1px solid $gray-900;
        }
      }
    }
  }
  &.table-row-highlighting {
    tbody {
      tr {
        &:hover {
          background-color: $blue-100;
        }
      }
    }
  }

  @media (max-width: 892px) {
    thead {
      @include visuallyHidden;
    }

    tbody {
      tr {
        td {
          display: grid;
          grid-template-columns: 1fr 2fr;
          justify-content: flex-start;
          align-items: center;
          padding: 6px 0;

          .cell-colname {
            @include revealHiddenContent;
            padding: 4px 12px;
            text-transform: capitalize;
          }

          &.value-number {
            text-align: left;
          }
        }
        &:last-child {
          td {
            border-bottom: none;
          }
        }
      }
    }
  }
}
</style>
