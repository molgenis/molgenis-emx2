<template>
  <div :style="table.drop ? 'text-decoration: line-through' : ''">
    <div>
      <div>
        <span class="hoverContainer">
          <h4
              :id="table.name != undefined ? table.name.replaceAll(' ', '_') : ''"
              style="display: inline-block; text-transform: none !important"
              :style="table.drop ? 'text-decoration: line-through' : ''"
          >
            Table: {{ table.name }}
          </h4>
          <TableEditModal v-model="table" @input="$emit('input', table)"/>
        </span>
        <span v-if="table.subclasses === undefined" class="hoverContainer">
            (no subclasses)  <IconAction
            icon="plus"
            @click="createSubclass"
            class="btn-sm hoverIcon"
        />
          </span>
        <div v-else>
          <span class="hoverContainer">
          Subclasses:
          <IconAction
              icon="plus"
              @click="createSubclass"
              class="btn-sm hoverIcon"
          /></span>
          <ul>
            <li v-for="(subclass, index) in table.subclasses" class="hoverContainer"
                :key="table.subclasses.length + '_' + index">
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
              {{ subclass.name }} extends {{ subclass.inherit }}<span
                v-if="subclass.description !== undefined">: {{ subclass.description }}</span>
            </li>
          </ul>
        </div>
        <p>
          {{
            table.description ? table.description : "No description available"
          }}
        </p>
        <div class="definition">{{ table.semantics }}</div>
        </span>
      </div>
      <table class="table table-bordered">
        <thead>
        <tr class="hoverContainer">
          <th style="width: 25%">
            Column
            <IconAction
                icon="plus"
                @click="createColumn"
                class="btn-sm hoverIcon"
            />
          </th>
          <th style="width: 25%">Definition</th>
          <th>Description</th>
        </tr>
        </thead>
        <Draggable v-model="table.columns" tag="tbody" @end="applyPosition">
          <ColumnView
              class="moveHandle"
              v-for="(column, columnIndex) in table.columns"
              :key="column.name + '_'+ columnIndex + '_' + table.columns.length"
              :tableName="table.name"
              :subclasses="subclassNames"
              v-model="table.columns[columnIndex]"
              :schema="schema"
              :columnIndex="columnIndex"
              @input="$emit('input', table)"
              @createColumn="createColumn"
          />
        </Draggable>
      </table>
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
import {IconAction} from "molgenis-components";
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
  computed: {
    subclassNames() {
      if (this.table.subclasses) {
        return this.table.subclasses.map((subclass) => subclass.name);
      }
    },
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
    createColumn(position) {
      let newColumn = {
        name: undefined,
        columnType: "STRING",
        table: this.table.name,
      };
      if (position) {
        this.table.columns.splice(position, 0, newColumn);
      } else {
        this.table.columns.unshift(newColumn);
      }
      this.applyPosition();
      this.$emit("input", this.table);
    },
    createSubclass() {
      if (!this.table.subclasses) {
        //need to $set otherwise vue doesn't see the change
        this.$set(this.table, "subclasses", []);
      }
      this.table.subclasses.unshift({
        name: undefined,
        description: "n/a",
        inherit: this.table.name,
      });
      this.$emit("input", this.table);
    },
  },
  created() {
    this.table = this.value;
  },
};
</script>
