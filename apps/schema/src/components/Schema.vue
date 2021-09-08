<template>
  <div>
    <div class="row">
      <div v-if="tables" class="col-2">
        <div class="fixedContainer">
          <h1>Tables:</h1>
          <p
            v-for="table in tables.filter((t) => t.externalSchema == undefined)"
            :key="table.name"
          >
            <a v-scroll-to="'#' + table.name" href=".">{{ table.name }}</a>
          </p>
        </div>
      </div>
      <div class="col-10">
        <RouterLink to="/formeditor">TO FORM EDITOR (Alpha!)</RouterLink>
        <RouterLink to="/simple">TO INPLACE TABLE EDITOR (Alpha!)</RouterLink>
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
        <MessageError v-else-if="graphqlError">{{ graphqlError }}</MessageError>
        <Yuml v-else :schema="{ tables: tables }" />

        <div>
          {{ count }} tables found
          <IconAction v-if="canEdit" icon="plus" @click="tableAdd = true" />
          <div class="table-responsive" v-if="tables">
            <table class="table table-hover table-sm table-bordered">
              <tbody
                v-for="table in tables.filter(
                  (t) => t.externalSchema == undefined
                )"
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
                  <td colspan="4">
                    <h3
                      :id="table.name"
                      style="text-transform: none"
                      class="mt-3"
                    >
                      {{ table.name }}
                      <span
                        v-if="table.semantics"
                        style="font-size: small; text-transform: none"
                      >
                        <<i>semantics:{{ table.semantics }}</i
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
                  <th scope="col">semantics</th>
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
                  <td>
                    {{ column.name }}
                  </td>
                  <td>
                    <span>{{ column.columnType }}</span>
                    <span v-if="column.refTable">({{ column.refTable }})</span
                    >&nbsp;
                    <span v-if="column.required">required&nbsp;</span>
                    <span v-if="column.refLink">
                      refLink({{ column.refLink }})&nbsp;
                    </span>
                    <span v-if="column.refBack">
                      refBack({{ column.refBack }})&nbsp;
                    </span>
                  </td>
                  <td>{{ column.key }}</td>
                  <td>{{ column.description }}</td>
                  <td>{{ column.semantics }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fixedContainer {
  position: -webkit-sticky; /* Safari */
  position: sticky;
  top: 0;
}

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
import Yuml from "./Yuml";

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
    Yuml,
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
      graphqlError: null,
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
      this.graphqlError = null;
      this.loading = true;
      this.loading = true;
      this.schema = null;
      this.tables = null;
      request(
        "graphql",
        "{_schema{name,tables{name,inherit,externalSchema,description,semantics,columns{name,columnType,inherited,key,refSchema,refTable,refLink,refBack,required,description,semantics,validation,visible}}}}"
      )
        .then((data) => {
          this.schema = data._schema.name;
          this.tables = data._schema.tables;
        })
        .catch((graphqlError) => {
          this.graphqlError = graphqlError.response.errors[0].message;
          if (
            this.graphqlError.includes(
              "Field '_schema' in type 'Query' is undefined"
            )
          ) {
            this.graphqlError =
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
  },
  created() {
    this.loadSchema();
  },
  watch: {
    tableAdd() {
    },
    tableAlter() {
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
