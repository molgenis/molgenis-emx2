<template>
  <Accordion
    class="chart-builder"
    :is-open-by-default="true"
    :id="`${formId}-accordion`"
    :title="chartTitle ? `Chart: ${chartTitle}` : 'Chart'"
  >
    <fieldset class="chart-form">
      <div class="form-instructions">
        <legend class="form-title">
          {{ chartTitle ? `Chart: ${chartTitle}` : "Chart" }}
        </legend>
        <p class="form-description">
          To configure your chart, select a table, and then select the columns
          you would like to display. You may select up to three columns.
        </p>
      </div>
      <div class="chart-selector">
        <ChartTypeOptions
          :id="`chart-${formId}-type`"
          @change="onChartTypeInput"
        />
      </div>
      <div class="chart-field chart-table">
        <InputLabel
          :id="`chart-${formId}-table`"
          label="Select a data table"
          description="Pick a table in the database"
        />
        <select
          :id="`chart-${formId}-table`"
          :ref="setFormInputs"
          v-model="tableName"
          data-for="table"
        >
          <option disabled selected value=""></option>
          <option v-for="row in tables" :value="row.name">
            {{ row.label ? row.label : row.name }}
          </option>
        </select>
      </div>
      <div class="chart-field chart-axis chart-axis-x">
        <InputLabel
          :id="`chart-${formId}-axis-x`"
          label="Set x-axis variable"
          description="Choose the axis to plot along the x-axis"
        />
        <select
          :id="`chart-${formId}-axis-x`"
          :ref="setFormInputs"
          v-model="xVar"
          data-for="x"
        >
          <option disabled selected value=""></option>
          <option v-for="row in xVarColumns" :value="row.name">
            {{ row.label ? row.label : row.name }}
          </option>
        </select>
      </div>
      <div class="chart-field chart-axis chart-axis-y">
        <InputLabel
          :id="`chart-${formId}-axis-y`"
          label="Set y-axis variable"
          description="Choose the axis to plot along the y-axis"
        />
        <select
          :id="`chart-${formId}-axis-y`"
          :ref="setFormInputs"
          v-model="yVar"
          data-for="y"
        >
          <option disabled selected value=""></option>
          <option v-for="row in yVarColumns" :value="row.name">
            {{ row.label ? row.label : row.name }}
          </option>
        </select>
      </div>
      <div class="chart-field chart-axis chart-axis-group">
        <InputLabel
          :id="`chart-${formId}-axis-group`"
          label="Set groups"
          description="Choose a variable to group the data by"
        />
        <select
          :id="`chart-${formId}-axis-group`"
          ref="groupInput"
          v-model="groupVar"
          data-for="group"
        >
          <option disabled selected value=""></option>
          <option v-for="row in groupVarColumns" :value="row.name">
            {{ row.label ? row.label : row.name }}
          </option>
        </select>
      </div>
      <div class="chart-field chart-id">
        <InputLabel
          :id="`chart-${formId}-chart-id`"
          label="Set the chart identifier"
          description="Provide a unique ID for the chart"
        />
        <input
          type="text"
          :id="`chart-${formId}-chart-id`"
          :ref="setFormInputs"
          v-model="chartId"
          data-for="id"
        />
      </div>
      <div class="chart-field chart-context chart-title">
        <InputLabel
          :id="`chart-${formId}-title`"
          label="Set chart title"
          description="Enter a title that describes the chart"
        />
        <input
          type="text"
          :id="`chart-${formId}-title`"
          :ref="setFormInputs"
          v-model="chartTitle"
          data-for="title"
        />
      </div>
      <div class="chart-field chart-context chart-description">
        <InputLabel
          :id="`chart-${formId}-description`"
          label="Set chart description"
          description="Provide additional context for a chart. This may be useful if you would like to further describe groups or highlight an important finding."
        />
        <input
          type="text"
          :id="`chart-${formId}-description`"
          :ref="setFormInputs"
          v-model="chartDescription"
          data-for="description"
        />
      </div>
    </fieldset>
    <div class="chart-output">
      <h3 class="chart-builder-title preview-title">Preview</h3>
      <pre>
        <code>
          type: {{ chartType }}
          tableName: {{ tableName }}
          chartId: {{ chartId }}
          title: {{ chartTitle}}
          description: {{  chartDescription }}
          x: <template v-if="xVar">{{ xVar }} ({{ xVarType }}; {{ xVarClass }})</template>
          y: <template v-if="yVar">{{ yVar }} ({{ yVarType }}; {{ yVarClass }})</template>
          group: <template v-if="groupVar">{{ groupVar }} ({{ groupVarType }}; {{ groupVarClass }})</template>
        </code>
      </pre>
      <pre v-if="showChartPreview">{{ chartQuery }}</pre>
    </div>
  </Accordion>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { emxTypes } from "../utils/defaults";
import { generateIdString } from "../utils/index";
import { createQuery } from "../utils/graphql";
import { emxTypeAsDataClass } from "../utils/defaults";
import type { TableSchema, ColumnSchema } from "../utils/types";

import Accordion from "../components/display/Accordion.vue";
import InputLabel from "../components/forms/InputLabel.vue";
import ChartTypeOptions from "../app-components/settings_chart_type_selector.vue";

const props = withDefaults(defineProps<{ tables: TableSchema[] | null }>(), {
  tables: [],
});

let formId = ref<String>(generateIdString());
let formInputs = ref<Element[]>([]);
let groupInput = ref<Element | null>(null);

let chartId = ref<String | null>(null);
let chartType = ref<String | null>(null);
let tableName = ref<String | null>(null);
let xVar = ref<String | null>(null);
let yVar = ref<String | null>(null);
let groupVar = ref<String | null>(null);

let xVarType = ref<String | null>(null);
let yVarType = ref<String | null>(null);
let groupVarType = ref<String | null>(null);
let xVarClass = ref<String | null>(null);
let yVarClass = ref<String | null>(null);
let groupVarClass = ref<String | null>(null);

let chartTitle = ref<String | null>(null);
let chartDescription = ref<String | null>(null);

let selectedTable = ref<TableSchema>({ name: "", columns: [] });
let xVarColumns = ref<ColumnSchema[]>([]);
let yVarColumns = ref<ColumnSchema[]>([]);
let groupVarColumns = ref<ColumnSchema[]>([]);

let showChartPreview = ref<Boolean>(false);
let minimalQueryRequirement: Number = 4;

let chartQuery = ref<String | null>(null);

function setFormInputs(element: Element) {
  formInputs.value.push(element);
}

function resetForm() {
  chartId.value = null;
  chartType.value = null;
  tableName.value = null;
  xVar.value = null;
  yVar.value = null;
  groupVar.value = null;

  xVarType.value = null;
  yVarType.value = null;
  groupVarType.value = null;
  xVarClass.value = null;
  yVarClass.value = null;
  groupVarClass.value = null;

  chartTitle.value = null;
  chartDescription.value = null;

  selectedTable.value = { name: "", columns: [] };
  xVarColumns.value = [];
  yVarColumns.value = [];
  groupVarColumns.value = [];

  chartQuery.value = "";
}

function onChartTypeInput(value: string) {
  resetForm();
  chartType.value = value;

  if (value === "columnChart") {
    groupInput.value.removeAttribute("disabled");
  } else {
    groupInput.value.setAttribute("disabled", "");
  }
}

function getVarColumnType(name: String) {
  const columns = selectedTable.value.columns;
  const row = columns.filter((row: ColumnSchema) => row.name === name)[0];
  return row.columnType;
}

function setColumnInputs() {
  const columns = selectedTable.value.columns;

  const continuousTableCols: Array<ColumnSchema[]> = columns.filter(
    (row: ColumnSchema) => {
      return emxTypes.continuous.includes(row.columnType);
    }
  );

  const categoricalTableCols: Array<ColumnSchema[]> = columns.filter(
    (row: ColumnSchema) => {
      return emxTypes.categorical.includes(row.columnType);
    }
  );

  if (chartType.value === "barChart") {
    xVarColumns.value = continuousTableCols;
    yVarColumns.value = categoricalTableCols;
  }

  if (["columnChart", "pieChart"].includes(chartType.value)) {
    xVarColumns.value = categoricalTableCols;
    yVarColumns.value = continuousTableCols;
    groupVarColumns.value = categoricalTableCols;
  }

  if (chartType.value === "scatterPlot") {
    xVarColumns.value = continuousTableCols;
    yVarColumns.value = continuousTableCols;
  }
}

watch(tableName, (name: string) => {
  if (name) {
    const table = props.tables.filter((tbl) => tbl.name === name)[0];
    selectedTable.value = table;
    setColumnInputs();
  }
});

watch(xVar, (value: string) => {
  if (value) {
    xVarType.value = getVarColumnType(value);
    xVarClass.value = emxTypeAsDataClass(xVarType.value);
  }
});

watch(yVar, (value: string) => {
  if (value) {
    yVarType.value = getVarColumnType(value);
    yVarClass.value = emxTypeAsDataClass(yVarType.value);
  }
});

watch(groupVar, (value: string) => {
  if (value) {
    groupVarType.value = getVarColumnType(value);
    groupVarClass.value = emxTypeAsDataClass(groupVarType.value);
  }
});

watch([chartType, tableName, chartId], (values: Array<String>) => {
  if (values[0] && values[1]) {
    chartId.value = `${chartType.value}-${formId.value}`;
  }
  if (values[2]) {
    chartId.value = values[2];
  }
});

watch([chartType, tableName, xVar, yVar, groupVar], (params: Array<String>) => {
  if (params) {
    const progress = params
      .map((param: String) => (param ? 1 : 0))
      .reduce((sum: number, value: number) => sum + value, 0);
    console.log(progress);
    showChartPreview.value = progress >= minimalQueryRequirement;

    chartQuery.value = createQuery({
      table: tableName.value,
      x: { column: xVar.value, columnType: xVarType.value },
      y: { column: yVar.value, columnType: yVarType.value },
      group: { column: groupVar.value, columnType: groupVarType.value },
    });
  }
});

//   chartConfig,
//   () => {
//     const currentProgress: Number = Object.keys(chartConfig.value)
//       .map((key: string) => {
//         return chartConfig.value[key] !== "" ? 1 : 0;
//       })
//       .reduce((sum, value) => sum + value, 0);
//     showChartPreview.value = currentProgress >= showChartThreshold.value;

//     chartQuery.value = createQuery({
//       table: chartConfig.value.table,
//       x: { column: chartConfig.value.x, columnType: chartConfig.value.x_type },
//       y: { column: chartConfig.value.y, columnType: chartConfig.value.y_type },
//     });
//   },
//   { deep: true }
// );

// watch(showChartPreview, (status: Boolean) => {
//   if (status) {
//     console.log("fetching data....");
//   }
// });
</script>

<style lang="scss">
.chart-builder {
  &.accordion {
    background-color: $gray-000;

    .accordion-heading {
      background-color: transparent;
    }

    .accordion-content {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1em;
      padding: 1em;
    }
  }
}

.chart-form {
  counter-reset: formItem;
  display: grid;
  grid-template-areas:
    "instructions instructions instructions"
    "chartType chartType chartType"
    "table table table"
    "axis-x axis-y axis-group"
    "title title title"
    "description description description"
    "id id id";
  gap: 0 1em;

  .chart-field {
    label {
      margin: 0;
      margin-bottom: 0.3em;

      &::before {
        counter-increment: formItem;
        content: counter(formItem) ".";
        margin-right: 0.1em;
        color: currentColor;
      }

      span {
        margin: 0;
      }
    }
  }

  .form-instructions {
    grid-area: instructions;

    p {
      font-size: 1rem;
      margin: 0;
    }
  }

  .chart-selector {
    grid-area: chartType;
  }

  .chart-table {
    grid-area: table;
  }

  .chart-axis-x {
    grid-area: axis-x;
  }

  .chart-axis-y {
    grid-area: axis-y;
  }

  .chart-axis-group {
    grid-area: axis-group;
  }

  .chart-title {
    grid-area: title;
  }

  .chart-description {
    grid-area: description;
  }

  .chart-id {
    grid-area: id;
  }

  .chart-context {
    label {
      margin: 0;
      span {
        margin: 0;
      }
    }
  }

  .chart-field {
    margin-top: 1em;

    input,
    select {
      width: 100%;
      @include textInput;
      margin: 0;
      font-size: 1rem;

      &:focus {
        @include inputFocus;
      }

      &:disabled {
        background-color: $gray-100;
      }
    }
  }
}

.chart-output {
  box-sizing: border-box;
  padding: 1em;
  background-color: $gray-050;
  pre {
    code {
      white-space: pre-line;
    }
  }
}
</style>
