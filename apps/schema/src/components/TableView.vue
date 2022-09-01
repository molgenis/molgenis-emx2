<template>
  <div :style="table.drop ? 'text-decoration: line-through' : ''">
    <div>
      <div>
        <span class="hoverContainer">
          <h4
            :id="
              table.name !== undefined ? table.name.replaceAll(' ', '_') : ''
            "
            style="display: inline-block; text-transform: none !important"
            :style="table.drop ? 'text-decoration: line-through' : ''"
          >
            Table: {{ table.name }}
            <span v-if="table.semantics" class="small">
              (<a
                :href="purl"
                target="_blank"
                v-for="purl in table.semantics"
                :key="purl"
                >{{ purl.substring(purl.lastIndexOf("/") + 1) }}</a
              >)
            </span>
          </h4>
          <TableEditModal
            v-model="table"
            :schema="schema"
            @input="$emit('input', table)"
          />
          <IconDanger
            @click="deleteTable(table)"
            icon="trash"
            class="hoverIcon"
          />
        </span>
        <p>
          {{
            table.description ? table.description : "No description available"
          }}
        </p>
        <span v-if="table.subclasses === undefined" class="hoverContainer">
          (no subclasses)
          <IconAction
            icon="plus"
            @click="createSubclass"
            class="btn-sm hoverIcon"
          />
        </span>
        <div v-else>
          <span class="hoverContainer">
            <h5>
              Subclasses:
              <IconAction
                icon="plus"
                @click="createSubclass"
                class="btn-sm hoverIcon"
              />
            </h5>
          </span>
          <table class="table table-bordered">
            <thead>
              <th style="width: 25%" scope="col">Subclass</th>
              <th style="width: 25%" scope="col">extends</th>
              <th>description</th>
            </thead>
            <tbody>
              <tr
                v-for="(subclass, index) in table.subclasses"
                class="hoverContainer"
                :key="table.subclasses.length + '_' + index"
                :style="subclass.drop ? 'text-decoration: line-through' : ''"
                :id="
                  subclass.name !== undefined
                    ? subclass.name.replaceAll(' ', '_')
                    : ''
                "
              >
                <td>
                  {{ subclass.name }}
                  <TableEditModal
                    v-model="table.subclasses[index]"
                    :schema="schema"
                    :rootTable="table"
                    @input="$emit('input', table)"
                  />
                  <IconDanger
                    @click="deleteTable(subclass)"
                    icon="trash"
                    class="hoverIcon"
                  />
                </td>
                <td>extends {{ subclass.inherit }}</td>
                <td>
                  {{ subclass.description }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <h5 class="hoverContainer">
        Columns:
        <IconAction
          icon="plus"
          @click="createColumn"
          class="btn-sm hoverIcon"
        />
      </h5>
      <table class="table table-bordered">
        <thead>
          <tr class="hoverContainer">
            <th style="width: 25%" scope="col">Column</th>
            <th style="width: 25%" scope="col">Definition</th>
            <th scope="col">Description</th>
          </tr>
        </thead>
        <Draggable v-model="table.columns" tag="tbody" @end="applyPosition">
          <ColumnView
            v-for="(column, columnIndex) in table.columns"
            :key="
              JSON.stringify(column) +
              '_' +
              columnIndex +
              '_' +
              table.columns.length
            "
            :style="
              isSubclassDropped(column) ? 'text-decoration: line-through' : ''
            "
            v-model="table.columns[columnIndex]"
            :schema="schema"
            :schemaNames="schemaNames"
            @input="$emit('input', table)"
            @createColumn="createColumn"
          />
        </Draggable>
      </table>
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
</style>

<script>
import { IconAction, IconDanger } from "molgenis-components";
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
    IconDanger,
  },
  props: {
    value: {
      type: Object,
      required: true,
    },
    schema: {
      type: Object,
      required: true,
    },
    schemaNames: {
      type: Array,
      required: true,
    },
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
      if (this.schema.tables.filter((t) => t.name === this.name).length > 1) {
        return "Table name must be unique within schema";
      }
    },
    deleteTable(table) {
      if (!table.drop) {
        //need to do deep set otherwise vue doesn't see it
        this.$set(table, "drop", true);
      } else {
        this.$set(table, "drop", false);
      }
    },
    isSubclassDropped(column) {
      if (column.table === this.table.name) {
        return this.table.drop;
      } else {
        return this.table.subclasses?.find(
          (subclass) => subclass.name === column.table
        ).drop;
      }
    },
    createColumn(position) {
      const newColumn = {
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
