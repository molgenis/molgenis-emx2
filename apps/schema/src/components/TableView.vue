<template>
  <div :style="table.drop ? 'text-decoration: line-through' : ''">
    <div>
      <div>
        <span class="hoverContainer">
          <h4
            :id="table.id !== undefined ? table.name.replaceAll(' ', '_') : ''"
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
            v-if="isManager"
            v-model="table"
            :schema="schema"
            @update:modelValue="$emit('update:modelValue', table)"
            :locales="locales"
          />
          <IconDanger
            v-if="isManager"
            @click="deleteTable"
            icon="trash"
            class="hoverIcon"
          />
          <a
            class="hoverIcon"
            :href="'#'"
            v-scroll-to="{
              el: '#molgenis_tables_container',
              offset: -200,
            }"
          >
            scroll to top
          </a>
        </span>
        <div v-if="table.labels">
          <label class="mb-0">Label: </label>
          <table class="table-borderless ml-4">
            <tr v-for="el in table.labels.filter((el) => el.value)">
              <td>{{ el.locale }}:</td>
              <td>{{ el.value }}</td>
            </tr>
          </table>
        </div>
        <div v-if="table.descriptions">
          <label class="mb-0">Description: </label>
          <table class="table-borderless ml-4">
            <tr v-for="el in table.descriptions.filter((el) => el.value)">
              <td>{{ el.locale }}:</td>
              <td>{{ el.value }}</td>
            </tr>
          </table>
        </div>

        <div v-if="table.tableType !== 'ONTOLOGIES'">
          <div>
            <div class="hoverContainer mb-2">
              <label style="display: inline">Subclasses:</label>
              <span
                v-if="table.subclasses === undefined"
                class="hoverContainer"
              >
                None
              </span>
              <TableEditModal
                v-if="isManager"
                :schema="schema"
                operation="add"
                :rootTable="table"
                @add="createSubclass"
                :locales="locales"
              />
            </div>
            <table
              class="table table-bordered table-sm"
              v-if="table.subclasses"
            >
              <thead>
                <th style="width: 25ch" scope="col">subclass</th>
                <th style="width: 25ch" scope="col">extends</th>
                <th scope="col">description</th>
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
                      v-if="isManager"
                      v-model="table.subclasses[index]"
                      :schema="schema"
                      :rootTable="table"
                      @update:modelValue="$emit('update:modelValue', table)"
                    />
                    <IconDanger
                      v-if="isManager"
                      @click="deleteSubclass(subclass)"
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
          <div class="hoverContainer mb-2">
            <label style="display: inline">Columns:</label>
            <span v-if="!table.columns?.length > 0"> None.</span>
            <ColumnEditModal
              v-if="isManager"
              :schema="schema"
              :schemaIds="schemaIds"
              operation="add"
              :tableName="table.name"
              :columnIndex="0"
              @add="addColumn(0, $event)"
              :locales="locales"
            />
          </div>
          <table
            v-if="table.columns?.length > 0"
            class="table table-bordered table-sm"
            style="table-layout: fixed"
          >
            <thead>
              <tr class="hoverContainer">
                <th style="width: 25ch" scope="col">column</th>
                <th
                  style="width: 25ch"
                  scope="col"
                  v-if="table.subclasses?.length > 0"
                >
                  inSubclass
                </th>
                <th style="width: 32ch" scope="col">definition</th>
                <th scope="col">label</th>
                <th scope="col">description</th>
              </tr>
            </thead>
            <Draggable
              v-model="table.columns"
              tag="tbody"
              @end="applyPosition"
              item-key="name"
              handle=".moveHandle"
              :disabled="!isManager"
            >
              <template #item="{ element, index }">
                <ColumnView
                  :style="
                    isSubclassDropped(element)
                      ? 'text-decoration: line-through'
                      : ''
                  "
                  v-model="table.columns[index]"
                  :schema="schema"
                  :schemaIds="schemaIds"
                  @update:modelValue="updateColumn(index, $event)"
                  @add="addColumn(index + 1, $event)"
                  @delete="deleteColumn(index)"
                  :columnIndex="index"
                  :isManager="isManager"
                  :locales="locales"
                />
              </template>
            </Draggable>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hoverIcon {
  visibility: hidden;
}

.hoverContainer:hover .hoverIcon {
  visibility: visible;
}
</style>

<script>
import { deepClone, IconDanger } from "molgenis-components";
import columnTypes from "../columnTypes.js";
import ColumnView from "./ColumnView.vue";
import Draggable from "vuedraggable";
import TableEditModal from "./TableEditModal.vue";
import ColumnEditModal from "./ColumnEditModal.vue";

export default {
  components: {
    TableEditModal,
    ColumnView,
    Draggable,
    IconDanger,
    ColumnEditModal,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
    },
    schema: {
      type: Object,
      required: true,
    },
    schemaIds: {
      type: Array,
      required: true,
    },
    isManager: {
      type: Boolean,
      default: false,
    },
    locales: {
      type: Array,
    },
  },
  data() {
    return {
      table: {},
      columnTypes,
    };
  },
  methods: {
    updateColumn(index, column) {
      this.table.columns.splice(index, 1, column);
      this.$emit("update:modelValue", this.table);
    },
    addColumn(index, column) {
      if (this.table.columns === undefined) {
        this.table.columns = [];
      }
      this.table.columns.splice(index ? index : 0, 0, column);
      this.applyPosition();
      this.$emit("update:modelValue", this.table);
    },
    applyPosition() {
      let position = 1;
      this.table.columns.forEach((column) => (column.position = position++));
      this.$emit("update:modelValue", this.table);
    },
    validateName() {
      if (!this.name) {
        return "Table name is required";
      }
      if (this.schema.tables.filter((t) => t.name === this.name).length > 1) {
        return "Table name must be unique within schema";
      }
    },
    deleteTable() {
      if (!this.table.oldName) {
        this.$emit("delete");
      } else if (!this.table.drop) {
        //need to do deep set otherwise vue doesn't see it
        this.table.drop = true;
      } else {
        this.table.drop = false;
      }
      this.$emit("update:modelValue", this.table);
    },
    deleteColumn(index) {
      if (this.table.columns[index].oldName === undefined) {
        this.table.columns.splice(index, 1);
      } else {
        this.table.columns[index].drop = !this.table.columns[index].drop;
      }
      this.$emit("update:modelValue", this.table);
    },
    deleteSubclass(subclass) {
      if (!subclass.oldName) {
        this.table.subclasses = this.table.subclasses.filter(
          (sb) => sb !== subclass
        );
      } else if (!subclass.drop) {
        //need to do deep set otherwise vue doesn't see it
        subclass.drop = true;
      } else {
        subclass.drop = false;
      }
      this.$emit("update:modelValue", this.table);
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
    createSubclass(subclass) {
      if (!this.table.subclasses) {
        //need to $set otherwise vue doesn't see the change
        this.table.subclasses = [];
      }
      this.table.subclasses.unshift(subclass);
      this.$emit("update:modelValue", this.table);
    },
  },
  created() {
    this.table = deepClone(this.modelValue);
  },
  emits: ["update:modelValue", "delete"],
};
</script>
