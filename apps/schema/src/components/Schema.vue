<template>
  <div>
    <RouterLink to="/formeditor">TO FORM EDITOR (Alpha!)</RouterLink>
    <TableEditModal
      v-if="tableAdd"
      :schema="schema"
      @close="
        tableAdd = false;
        loadSchema();
      "
    />
    <TableEditModal
      v-if="tableAlter"
      :schema="schema"
      :table="currentTable"
      @close="
        tableAlter = false;
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
      :metadata="tables"
      :schema="schema"
      :table="currentTable.name"
      @close="
        columnAlter = false;
        loadSchema();
      "
    />
    <ColumnEditModal
      v-if="columnAdd"
      :metadata="tables"
      :schema="schema"
      :show="true"
      :table="currentTable.name"
      @close="
        columnAdd = false;
        loadSchema();
      "
    />
    <ColumnDropModal
      v-if="columnDrop"
      :column="currentColumn.name"
      :schema="schema"
      :table="currentTable.name"
      @close="
        columnDrop = false;
        loadSchema();
      "
    />
    <Spinner v-if="loading" />
    <MessageError v-else-if="error">{{ error }}</MessageError>
    <div v-else :class="{ 'img-fullscreen': imgFullscreen }">
      <div v-if="tables">
        <IconAction
          v-if="!loadingYuml"
          :icon="imgFullscreen ? 'compress' : 'expand'"
          class="fullscreen-icon"
          @click="imgFullscreen = !imgFullscreen"
        />
        <h1>
          Schema: <span style="text-transform: none">{{ schema }}</span>
        </h1>
        <InputCheckbox
          v-model="showAttributes"
          :defaultValue="showAttributes"
          :options="['attributes', 'external', 'inheritance']"
        />
        <Spinner v-if="loadingYuml" />
        <div
          v-scroll-lock="imgFullscreen"
          style="text-align: center; overflow: auto"
        >
          <img
            :key="JSON.stringify(showAttributes)"
            :src="yuml"
            :style="{
              visibility: loadingYuml ? 'hidden' : 'visible',
              'max-width': imgFullscreen ? 'none' : '100%',
            }"
            alt="Small"
            @load="loadingYuml = false"
          />
        </div>
      </div>
    </div>
    <div v-if="tables">
      <span
        v-for="table in tables.filter((t) => t.externalSchema == undefined)"
        :key="table.name"
      >
        <a v-scroll-to="'#' + table.name" href=".">{{ table.name }}</a> |
      </span>
    </div>
    <div>
      {{ count }} tables found
      <IconAction v-if="canEdit" icon="plus" @click="tableAdd = true" />
      <div class="table-responsive" v-if="tables">
        <table class="table table-hover table-sm">
          <tbody
            v-for="table in tables.filter((t) => t.externalSchema == undefined)"
            :key="table.name"
          >
            <tr>
              <td>
                <IconBar class="text-nowrap mt-4">
                  <IconAction
                    icon="edit"
                    @click="
                      currentTable = table;
                      tableAlter = true;
                    "
                  />
                  <IconDanger
                    icon="trash"
                    @click="
                      currentTable = table;
                      tableDrop = true;
                    "
                  />
                </IconBar>
              </td>
              <td colspan="5">
                <h3 :id="table.name" style="text-transform: none" class="mt-3">
                  {{ table.name }}
                  <span
                    v-if="table.jsonldType"
                    style="font-size: small; text-transform: none"
                  >
                    <<i>jsonldType:{{ table.jsonldType }}</i
                    >> <br />
                  </span>
                </h3>
                <small v-if="table.description">
                  <i>Description: {{ table.description }}</i></small
                >
              </td>
              <td><a v-scroll-to="'#__top'" href=".">back to top</a></td>
            </tr>
            <tr>
              <th scope="col">
                <IconAction
                  icon="plus"
                  @click="
                    currentTable = table;
                    columnAdd = true;
                  "
                />
              </th>
              <th scope="col">name</th>
              <th scope="col">type</th>
              <th scope="col">key</th>
              <th scope="col">description</th>
              <th scope="col">jsonldType</th>
            </tr>
            <tr
              v-for="column in table.columns == null
                ? []
                : table.columns.filter((c) => {
                    return c.name != 'mg_tableclass' && !c.inherited;
                  })"
              :key="column.name"
            >
              <td>
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
              </td>
              <td>{{ column.name }}<span v-if="!column.nillable">*</span></td>
              <td>
                <span>{{ column.columnType }}</span>
                <span v-if="column.refTable">({{ column.refTable }})</span
                >&nbsp;
                <span v-if="column.nullable">nullable&nbsp;</span>
                <span v-if="column.cascadeDelete">cascadeDelete&nbsp;</span>
              </td>
              <td>{{ column.key }}</td>
              <td>{{ column.description }}</td>
              <td>{{ column.jsonldType }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
@media (hover: hover) {
  .hover {
    opacity: 0;
  }
}

.hover {
  float: left;
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

.img-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  padding: 10px;
  overflow: auto;
  background: rgb(250, 250, 250);
}

.fullscreen-icon {
  float: right;
  top: 0px;
  right: 0px;
}
</style>

<script>
import { request } from "graphql-request";
import Vue from "vue";
import VScrollLock from "v-scroll-lock";
import {
  IconAction,
  IconBar,
  IconDanger,
  InputBoolean,
  InputCheckbox,
  MessageError,
  Molgenis,
  Spinner,
} from "@mswertz/emx2-styleguide";
import ColumnEditModal from "./ColumnEditModal";
import ColumnDropModal from "./ColumnDropModal";
import TableEditModal from "./TableEditModal";
import TableDropModal from "./TableDropModal";

import VueScrollTo from "vue-scrollto";

Vue.use(VScrollLock);
Vue.use(VueScrollTo);

export default {
  components: {
    IconBar,
    IconAction,
    IconDanger,
    Spinner,
    MessageError,
    InputBoolean,
    InputCheckbox,
    TableEditModal,
    ColumnEditModal,
    ColumnDropModal,
    TableDropModal,
    Molgenis,
  },
  props: {
    session: Object,
  },
  data: function () {
    return {
      schema: null,
      showAttributes: [],
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
      tableAlter: false,
      tableDrop: false,
      imgFullscreen: false,
    };
  },
  methods: {
    // alter(column) {},
    // drop(column) {},
    loadSchema() {
      this.error = null;
      this.loading = true;
      this.schema = null;
      this.tables = null;
      request(
        "graphql",
        "{_schema{name,tables{name,inherit,externalSchema,description,jsonldType,columns{name,columnType,columnFormat,inherited,key,refSchema,refTable,refFrom,refTo,mappedBy,cascadeDelete,nullable,description,jsonldType,validationExpression,visibleExpression}}}}"
      )
        .then((data) => {
          this.schema = data._schema.name;
          this.tables = data._schema.tables;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
          if (
            this.error.includes("Field '_schema' in type 'Query' is undefined")
          ) {
            this.error =
              "Schema is unknown or permission denied (might you need to login with authorized user?)";
          }
        })
        .finally((this.loading = false));
    },
  },
  computed: {
    canEdit() {
      return (
        this.session != null &&
        (this.session.email == "admin" ||
          (this.session.roles &&
            (this.session.roles.includes("Editor") ||
              this.session.roles.includes("Manager"))))
      );
    },
    count() {
      if (this.tables)
        return this.tables.filter((t) => t.externalSchema == undefined).length;
      return 0;
    },
    yuml() {
      this.loadingYuml = true;
      if (!this.tables) return "";
      let res = "http://yuml.me/diagram/plain;dir:bt/class/";
      // classes
      this.tables
        .filter(
          (t) => !t.externalSchema || this.showAttributes.includes("external")
        )
        .forEach((table) => {
          res += `[${table.name}`;

          if (
            Array.isArray(table.columns) &&
            this.showAttributes.includes("attributes")
          ) {
            res += "|";
            table.columns
              .filter((column) => !column.inherited)
              .forEach((column) => {
                if (column.columnType.includes("REF")) {
                  res += `${column.name}:${column.refTable}`;
                } else {
                  res += `${column.name}:${column.columnType}`;
                }
                res += `［${column.nullable ? "0" : "1"}..${
                  column.columnType.includes("ARRAY") ? "*" : "1"
                }］;`; //notice I use not standard [] to not break yuml
              });
          }
          if (table.externalSchema) {
            res += `],`;
          } else {
            res += `{bg:dodgerblue}],`;
          }
        });

      // relations
      this.tables
        .filter(
          (t) =>
            t.externalSchema == undefined ||
            this.showAttributes.includes("external")
        )
        .forEach((table) => {
          if (table.inherit && this.showAttributes.includes("inheritance")) {
            res += `[${table.inherit}]^-[${table.name}],`;
          }
          if (Array.isArray(table.columns)) {
            table.columns
              .filter(
                (c) =>
                  !c.inherited &&
                  (c.refSchema == undefined ||
                    this.showAttributes.includes("external"))
              )
              .forEach((column) => {
                if (column.columnType === "REF") {
                  console.log(JSON.stringify(column));

                  res += `[${table.name}]->[${column.refTable}],`;
                } else if (column.columnType === "REF_ARRAY") {
                  console.log(JSON.stringify(column));

                  res += `[${table.name}]-*>[${column.refTable}],`;
                }
              });
          }
        });

      return res;
    },
  },
  created() {
    this.loadSchema();
  },
  watch: {
    tableAdd() {
      console.log("tableadd changed " + JSON.stringify(this.tableAdd));
    },
    tableAlter() {
      console.log("tableAlter changed " + JSON.stringify(this.tableAlter));
    },
    session: {
      deep: true,
      handler() {
        this.loadSchema();
      },
    },
  },
};
</script>

<docs>
Example
```
<Schema schema="pet store"/>
```
</docs>
