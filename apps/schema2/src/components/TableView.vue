<template>
  <div>
    <div>
      <div>
        <span class="hoverContainer">
          <h4
            :id="table.name"
            style="display: inline-block; text-transform: none !important"
            :style="table.drop ? 'text-decoration: line-through' : ''"
          >
            {{ table.name }}
          </h4>
          <TableEditModal v-model="table" @input="$emit('input', table)" />
          <div class="mb-2 row">
            <div class="col-2"><label>Description:</label></div>
            <div class="col">{{ table.description }}</div>
            <br />
            <div class="definition">{{ table.semantics }}</div>
          </div>
        </span>
        <div class="row hoverContainer">
          <div class="col">
            <label>Subclasses:</label>
            <IconAction
              icon="plus"
              @click="createSubclass"
              class="btn-sm hoverIcon"
            />
            <div class="mb-2">
              <div
                v-for="(subclass, index) in table.subclasses"
                class="row mb-2"
                :key="index"
              >
                <div class="col-2">
                  <span class="pl-2"> {{ subclass.name }}</span>
                  <TableEditModal
                    v-model="table.subclasses[index]"
                    @input="$emit('input', table)"
                    :extendsOptions="[
                      table.name,
                      ...table.subclasses
                        .map((subclass) => subclass.name)
                        .filter((t) => t != subclass.name),
                    ]"
                  />
                </div>
                <div class="col">
                  {{ subclass.description }}
                  <div class="definition">extends({{ subclass.inherit }})</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="row hoverContainer">
          <div class="col">
            <label>Columns:</label>
            <IconAction
              icon="plus"
              @click="createColumn"
              class="btn-sm hoverIcon"
            />
          </div>
        </div>
        <div>
          <Draggable v-model="table.columns" @end="applyPosition()">
            <ColumnView
              class="moveHandle"
              v-for="(column, columnIndex) in table.columns"
              :key="columnIndex"
              :tableName="table.name"
              v-model="table.columns[columnIndex]"
              :schema="schema"
              :columnIndex="columnIndex"
              @input="$emit('input', table)"
            />
          </Draggable>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.hoverIcon {
  visibility: hidden;
}

.hoverContainer:hover .hoverIcon {
  visibility: visible;
}

.moveHandle:hover {
  cursor: move;
}
</style>

<script>
import { IconAction } from "molgenis-components";
import columnTypes from "../columnTypes.js";
import ColumnView from "./ColumnView.vue";
import Draggable from "vuedraggable";
import TableEditModal from "./TableEditModal.vue";

export default {
  components: {
    TableEditModal,
    IconAction,
    ColumnView,
    Draggable,
  },
  props: {
    value: Object,
    schema: Object,
  },
  data() {
    return {
      table: {},
      columnTypes,
    };
  },
  methods: {
    applyPosition() {
      let position = 1;
      this.table.columns.forEach((column) => (column.position = position++));
      this.$emit("input", this.table);
    },
    validateName() {
      if (!this.name) {
        return "Table name is required";
      }
      if (this.schema.tables.filter((t) => t.name == this.name).length > 1) {
        return "Table name must be unique within schema";
      }
    },
    createColumn() {
      this.table.columns.push({
        name: undefined,
        columnType: "STRING",
      });
    },
    createSubclass() {
      this.table.subclasses.push({
        name: "newtable",
        description: "n/a",
        extends: this.table.name,
      });
    },
  },
  created() {
    this.table = this.value;
  },
};
</script>
