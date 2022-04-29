<template>
  <div v-if="table">
    <div class="row">
      <div class="col bg-white">
        <div class="row bg-white column-hover">
          <div class="col pl-4 pr-4">
            <h2>
              <InputString :inplace="true" v-model="table.name" @input="emit" />
            </h2>
            <p>
              <InputText
                :inplace="true"
                v-model="table.description"
                @input="emit"
              />
            </p>
          </div>
        </div>
        <form>
          <Draggable :list="table.columns">
            <div v-for="(column, idx) in table.columns" :key="idx + changed">
              <div
                v-if="column.name != 'mg_tableclass'"
                class="row mt-1 pt-1 column-hover"
                :class="{'border border-primary': selectedColumn == idx}"
              >
                <div class="col bg-white">
                  <div>
                    <IconAction
                      class="hoverIcon float-right align-bottom"
                      :icon="selectedColumn == idx ? 'chevron-up' : 'cog'"
                      @click="
                        selectedColumn = selectedColumn == idx ? null : idx
                      "
                    />
                    <RowFormInput
                      v-if="visible(column.visibleIf)"
                      v-model="example[column.name]"
                      :editMeta="true"
                      :label.sync="column.name"
                      :description.sync="column.description"
                      :columnType="column.columnType"
                      :refTable="column.refTable"
                      :required="column.required"
                      :errorMessage="errorPerColumn[column.name]"
                      :key="idx + changetime"
                      class="pl-2 pr-2 mb-0"
                    />
                    <label v-else class="pl-2 pr-2 text-muted">
                      <i>
                        {{ column.name }} is not visible because of expression
                        '{{ column.visibleIf }}'
                      </i>
                    </label>
                  </div>
                  <div v-if="selectedColumn == idx" class="bg-light p-2">
                    <IconAction
                      icon="plus"
                      class="mr-2"
                      @click="
                        newcol = {
                          name: 'new'
                        };
                        newcol.columnType = 'STRING';
                        table.columns.push(newcol);
                      "
                    />
                    <IconAction
                      icon="trash"
                      class="mr-2"
                      @click="
                        column.drop = true;
                        table.columns = table.columns.filter(
                          (c) => c.name != column.name
                        );
                      "
                    />
                    <ColumnEdit
                      v-model="table.columns[idx]"
                      :table="table"
                      :schema="schema"
                      :hide-name-description="visible(column.visibleIf)"
                      @update="changed"
                    />
                  </div>
                </div>
              </div>
            </div>
          </Draggable>
        </form>
        <IconAction
          icon="plus"
          class="mr-2"
          @click="
            selectedColumn = {
              name: 'new'
            };
            selectedColumn.columnType = 'STRING';
            table.columns.push(selectedColumn);
            selectedColumnName = selectedColumn.name;
          "
        />
      </div>
    </div>
  </div>
</template>

<style>
.column-hover label:hover {
  cursor: grab;
}

.column-hover .hoverIcon {
  visibility: hidden;
}

.column-hover:hover .hoverIcon {
  visibility: visible;
}
</style>

<script>
import {
  IconAction,
  RowFormInput,
  InputString,
  InputText
} from "@mswertz/emx2-styleguide";
import Draggable from "vuedraggable";
import ColumnEdit from "./ColumnEdit";
import {EMAIL_REGEX, HYPERLINK_REGEX} from "../../../styleguide/src/constants";
import Expressions from "@molgenis/expressions";

export default {
  components: {
    ColumnEdit,
    IconAction,
    InputString,
    RowFormInput,
    InputText,
    Draggable
  },
  props: {
    schema: Object,
    /** metadata of a table*/
    value: Object
  },
  data() {
    return {
      table: {},
      example: {},
      selectedColumn: null,
      selectedColumnName: null,
      errorPerColumn: {},
      changetime: Date.now()
    };
  },
  computed: {
    tables() {
      if (!this.schema) {
        return [];
      }
      return this.schema.tables.map((t) => t.name);
    }
  },
  methods: {
    emit() {
      this.selectedColumn = null;
      this.validate();
      this.$emit("input", this.table);
    },
    changed() {
      this.changetime = Date.now();
    },
    eval(expression) {
      try {
        let args = Object.keys(this.example).join(",");
        let func = `(function (${args}) { return ${expression}; })`;
        return eval(func)(Object.values(this.example)); // eslint-disable-line
      } catch (e) {
        return "Error in validation script: " + e.message;
      }
    },
    visible(expression) {
      if (expression) {
        return this.eval(expression);
      } else {
        return true;
      }
    },
    validate() {
      this.table.columns.forEach((column) => {
        this.validateColumn(column);
      });
    },
    validateColumn(column) {
      const value = this.example[column.name];
      const isInvalidNumber = typeof value === "number" && isNaN(value);
      const missesValue = value === undefined || value === null || value === "";
      if (column.required && (missesValue || isInvalidNumber)) {
        this.errorPerColumn[column.name] = column.name + " is required ";
      } else if (missesValue) {
        this.errorPerColumn[column.name] = undefined;
      } else {
        this.errorPerColumn[column.name] = this.getColumnError(column);
      }
    },
    getColumnError(column) {
      const type = column.columnType;
      const value = this.example[column.name];

      if (type === "EMAIL" && !isValidEmail(value)) {
        return "Invalid email address";
      }
      if (type === "EMAIL_ARRAY" && containsInvalidEmail(value)) {
        return "Invalid email address";
      }
      if (type === "HYPERLINK" && !isValidHyperlink(value)) {
        return "Invalid hyperlink";
      }
      if (type === "HYPERLINK_ARRAY" && containsInvalidHyperlink(value)) {
        return "Invalid hyperlink";
      }
      if (column.validation) {
        return evaluateValidationExpression(column, this.example);
      }

      return undefined;
    }
  },
  watch: {
    example: {
      deep: true,
      handler() {
        this.validate();
      }
    },
    value() {
      this.table = this.value;
    }
  },
  created() {
    this.table = this.value;
  }
};

function isValidHyperlink(value) {
  return HYPERLINK_REGEX.test(String(value).toLowerCase());
}

function containsInvalidHyperlink(hyperlinks) {
  return hyperlinks.find((hyperlink) => !isValidHyperlink(hyperlink));
}

function isValidEmail(value) {
  return EMAIL_REGEX.test(String(value).toLowerCase());
}

function containsInvalidEmail(emails) {
  return emails.find((email) => !isValidEmail(email));
}

function evaluateValidationExpression(column, values) {
  try {
    if (!Expressions.evaluate(column.validation, values)) {
      return `Applying validation rule returned error: ${column.validation}`;
    }
  } catch (error) {
    return "Invalid validation expression " + error;
  }
}
</script>
