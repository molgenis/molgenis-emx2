<template>
  <Molgenis id="__top" v-model="session">
    <Spinner v-if="loading" />
    <MessageError v-else-if="error">{{ error }}</MessageError>
    <div v-else :class="{ 'img-fullscreen': imgFullscreen }">
      <div>
        <IconAction
          v-if="!loadingYuml"
          class="fullscreen-icon"
          :icon="imgFullscreen ? 'compress' : 'expand'"
          @click="imgFullscreen = !imgFullscreen"
        />
        <h1>Schema: {{ schema }}</h1>
        <InputBoolean
          v-model="showAttributes"
          label="show attributes"
          :defaultValue="showAttributes"
        />
        <Spinner v-if="loadingYuml" />
        <div
          style="text-align:center; overflow: auto;"
          v-scroll-lock="imgFullscreen"
        >
          <img
            :style="{
              visibility: loadingYuml ? 'hidden' : 'visible',
              'max-width': imgFullscreen ? 'none' : '100%'
            }"
            @load="loadingYuml = false"
            :src="yuml"
            :key="showAttributes"
            alt="Small"
          />
        </div>
      </div>
    </div>
    <span v-for="table in tables" :key="table.name">
      <a href="." v-scroll-to="'#' + table.name">{{ table.name }}</a> |
    </span>
    <div>
      {{ count }} tables found
      <IconAction v-if="canEdit" icon="plus" @click="tableAdd = true" />
      <div class="table-responsive">
        <table class="table table-hover">
          <tbody v-for="table in tables" :key="table.name">
            <tr>
              <td colspan="3">
                <h1 :id="table.name">
                  {{ table.name }}
                  <IconBar class="hover">
                    <!--IconAction
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        icon="edit"
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        @click="
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          currentTable = table;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          tableAdd = true;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      /-->
                    <IconDanger
                      v-if="canEdit"
                      icon="trash"
                      @click="
                        currentTable = table;
                        tableDrop = true;
                      "
                    />
                  </IconBar>
                </h1>
              </td>
              <td><a href="." v-scroll-to="'#__top'">back to top</a></td>
            </tr>
            <tr>
              <th scope="col">
                name
                <IconAction
                  v-if="canEdit"
                  icon="plus"
                  class="hover"
                  @click="
                    currentTable = table;
                    columnAdd = true;
                  "
                />
              </th>
              <th scope="col">type</th>
              <th scope="col">key</th>
              <th scope="col">description</th>
            </tr>
            <tr
              v-for="column in table.columns.filter(c => {
                return c.name != 'mg_tableclass' && !c.inherited;
              })"
              :key="column.name"
            >
              <td>
                {{ column.name }}<span v-if="!column.nillable">*</span>
                <span class="hover"
                  ><IconAction
                    v-if="canEdit"
                    icon="edit"
                    @click="
                      currentTable = table;
                      currentColumn = column;
                      columnAlter = true;
                    "/>
                  <IconDanger
                    v-if="canEdit"
                    icon="trash"
                    @click="
                      currentTable = table;
                      currentColumn = column;
                      columnDrop = true;
                    "
                /></span>
              </td>
              <td>
                <span>{{ column.columnType }}</span>
                <span v-if="column.refTable">({{ column.refTable }})</span
                >&nbsp;
                <span v-if="column.nullable">nullable&nbsp;</span>
                <span v-if="column.cascadeDelete">cascadeDelete&nbsp;</span>
              </td>
              <td>{{ column.key }}</td>
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
  MessageError,
  Molgenis,
  Spinner
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
    TableEditModal,
    ColumnEditModal,
    ColumnDropModal,
    TableDropModal,
    Molgenis
  },
  data: function() {
    return {
      session: null,
      schema: null,
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
      tableDrop: false,
      imgFullscreen: false
    };
  },
  methods: {
    // alter(column) {},
    // drop(column) {},
    loadSchema() {
      this.loading = true;
      request(
        "graphql",
        "{_schema{name,tables{name,inherit,description,columns{name,columnType,inherited,key,refTable,mappedBy,cascadeDelete,nullable,description,rdfTemplate}}}}"
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
      if (this.tables) return this.tables.length;
      return 0;
    },
    yuml() {
      this.loadingYuml = true;
      if (!this.tables) return "";
      let res = "http://yuml.me/diagram/plain;dir:td/class/";
      // classes
      this.tables.forEach(table => {
        if (table.inherit) {
          res += `[${table.inherit}]^-`;
        }
        res += `[${table.name}`;

        if (Array.isArray(table.columns) && this.showAttributes) {
          res += "|";
          table.columns
            .filter(column => !column.inherited)
            .forEach(column => {
              res += `${column.name};`;
            });
        }
        res += `],`;
      });
      // relations
      this.tables.forEach(table => {
        if (Array.isArray(table.columns)) {
          table.columns
            .filter(column => !column.inherited)
            .forEach(column => {
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
