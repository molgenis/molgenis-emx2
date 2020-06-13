<template>
  <Molgenis :menuItems="menuItems" :title="'Schema ' + schema" id="__top">
    <Spinner v-if="loading" />
    <MessageError v-else-if="error">{{ error }}</MessageError>
    <div v-else>
      <InputBoolean
        v-model="showAttributes"
        label="show attributes"
        :defaultValue="showAttributes"
      />
      <div style="overflow: auto; text-align:center">
        <Spinner v-if="loadingYuml" />
        <img
          :style="{ visibility: loadingYuml ? 'hidden' : 'visible' }"
          :src="yuml"
          :key="showAttributes"
          @load="loadingYuml = false"
        />
      </div>
      <div></div>
      <span v-for="table in tables" :key="table.name">
        <a href="." v-scroll-to="'#' + table.name">{{ table.name }}</a> |
      </span>
    </div>
    <div>
      {{ count }} tables found
      <IconAction icon="plus" @click="tableAdd = true" />
      <div class="table-responsive">
        <table class="table table-hover">
          <tbody v-for="table in tables" :key="table.name">
            <tr>
              <td colspan="3">
                <h1 :id="table.name">
                  {{ table.name }}
                  <IconBar class="hover">
                    <IconAction icon="edit" />
                    <IconDanger
                      icon="trash"
                      @click="
                        currentTable = table;
                        tableDrop = true;
                      "
                    />
                  </IconBar>
                </h1>
                <br />
                <a href="." v-scroll-to="'#__top'">back to top</a>
              </td>
            </tr>
            <tr>
              <th scope="col">
                name
                <IconAction
                  icon="plus"
                  class="hover"
                  @click="
                    currentTable = table;
                    columnAdd = true;
                  "
                />
              </th>
              <th scope="col">type</th>
              <th scope="col">description</th>
            </tr>
            <tr v-for="column in table.columns" :key="column.name">
              <td>
                {{ column.name }}
                <IconBar class="hover">
                  <IconAction
                    icon="edit"
                    @click="
                      currentTable = table;
                      currentColumn = column;
                      columnAlter = true;
                    "
                  />
                  <IconDanger
                    icon="trash"
                    @click="
                      currentTable = table;
                      currentColumn = column;
                      columnDrop = true;
                    "
                  />
                </IconBar>
              </td>
              <td>
                <span>{{ column.columnType }}</span>
                <span v-if="column.refTable"
                  >({{ column.refTable }}.{{ column.refColumn }})</span
                >&nbsp;
                <span v-if="column.nullable">nullable&nbsp;</span>
                <span v-if="column.cascadeDelete">cascadeDelete&nbsp;</span>
              </td>
              <td>{{ column.description }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <TableEditModal
      v-if="tableAdd"
      :schema="schema"
      @close="
        tableAdd = false;
        loadSchema();
      "
    />
    <TableDropModal
      v-if="tableDrop"
      :schema="schema"
      :table="currentTable.name"
      @close="
        tableDrop = false;
        loadSchema();
      "
    />
    <ColumnEditModal
      v-if="columnAlter"
      :defaultValue="currentColumn"
      :schema="schema"
      :table="currentTable.name"
      :metadata="tables"
      @close="
        columnAlter = false;
        loadSchema();
      "
    />
    <ColumnEditModal
      v-if="columnAdd"
      :schema="schema"
      :table="currentTable.name"
      :metadata="tables"
      :show="true"
      @close="
        columnAdd = false;
        loadSchema();
      "
    />
    <ColumnDropModal
      v-if="columnDrop"
      :schema="schema"
      :table="currentTable.name"
      :column="currentColumn.name"
      @close="
        columnDrop = false;
        loadSchema();
      "
    />
  </Molgenis>
</template>

<style scoped>
@media (hover: hover) {
  .hover {
    opacity: 0;
  }
}

.hover {
  float: right;
  white-space: nowrap;
}

h1 {
  display: inline-block;
}

th {
  font-weight: bold;
}

table tr:hover .hover {
  opacity: 1;
}

table th:hover .hover {
  opacity: 1;
}
</style>

<script>
import { request } from "graphql-request";
import Vue from "vue";
import VScrollLock from "v-scroll-lock";
import {
  IconBar,
  IconAction,
  IconDanger,
  Spinner,
  MessageError,
  InputBoolean,
  Molgenis
} from "@mswertz/emx2-styleguide";
import ColumnEditModal from "./ColumnEditModal";
import ColumnDropModal from "./ColumnDropModal";
import TableEditModal from "./TableEditModal";
import TableDropModal from "./TableDropModal";

import VueScrollTo from "vue-scrollto";

Vue.use(VScrollLock);
Vue.use(VueScrollTo);

export default {
  props: {
    schema: String
  },
  components: {
    IconBar,
    IconAction,
    IconDanger,
    Spinner,
    MessageError,
    InputBoolean,
    TableEditModal,
    ColumnEditModal,
    ColumnDropModal,
    TableDropModal,
    Molgenis
  },
  data: function() {
    return {
      showAttributes: false,
      loading: false,
      loadingYuml: false,
      tables: null,
      error: null,
      currentTable: null,
      currentColumn: null,
      columnAlter: false,
      columnAdd: false,
      columnDrop: false,
      tableAdd: false,
      tableDrop: false
    };
  },
  methods: {
    // alter(column) {},
    // drop(column) {},
    loadSchema() {
      this.loading = true;
      request(
        "graphql",
        "{_schema{name, tables{name,pkey,description,columns{name,columnType,pkey,refTable,refColumn,cascadeDelete,nullable,description}}}}"
      )
        .then(data => {
          this.schema = data._schema.name;
          this.tables = data._schema.tables;
        })
        .catch(error => {
          this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    }
  },
  computed: {
    menuItems() {
      return [
        { label: "Tables", href: "../tables/" },
        {
          label: "Schema",
          href: "../schema/"
        },
        {
          label: "Upload",
          href: "../import/"
        },
        {
          label: "Download",
          href: "../download/"
        },
        {
          label: "GraphQL",
          href: "/api/playground.html?schema=/api/graphql/" + this.schema
        },
        {
          label: "Settings",
          href: "../settings/"
        }
      ];
    },
    count() {
      if (this.tables) return this.tables.length;
      return 0;
    },
    yuml() {
      this.loadingYuml = true;
      if (!this.tables) return "";
      let res = "http://yuml.me/diagram/scruffy;dir:lr/class/";
      // classes
      this.tables.forEach(table => {
        res += `[${table.name}`;
        if (Array.isArray(table.columns) && this.showAttributes) {
          res += "|";
          table.columns.forEach(column => {
            res += `${column.name};`;
          });
        }
        res += `],`;
      });
      // relations
      this.tables.forEach(table => {
        if (Array.isArray(table.columns)) {
          table.columns.forEach(column => {
            if (column.columnType === "REF") {
              res += `[${table.name}]->[${column.refTable}],`;
            } else if (column.columnType === "REF_ARRAY") {
              res += `[${table.name}]-*>[${column.refTable}],`;
            }
          });
        }
      });
      return res;
    }
  },
  created() {
    this.loadSchema();
  }
};
</script>

<docs>
    Example
    ```
    <Schema schema="pet store"/>
    ```
</docs>
