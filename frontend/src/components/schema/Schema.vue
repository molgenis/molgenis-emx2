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
            <a href=".">{{ table.name }}</a>
          </p>
        </div>
      </div>
      <div class="col-10">
        <RouterLink to="/formeditor">
          TO FORM EDITOR (Alpha!)
        </RouterLink>
        <RouterLink to="/simple">
          TO INPLACE TABLE EDITOR (Alpha!)
        </RouterLink>
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
          :default-value="currentColumn"
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
        <MessageError v-else-if="graphqlError">
          {{ graphqlError }}
        </MessageError>
        <Yuml v-else :schema="{ tables: tables }" />

        <div>
          {{ count }} tables found
          <IconAction v-if="canEdit" icon="plus" @click="tableAdd = true" />
          <div v-if="tables" class="table-responsive">
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
                      class="mt-3"
                      style="text-transform: none;"
                    >
                      {{ table.name }}
                      <span
                        v-if="table.semantics"
                        style="font-size: small; text-transform: none;"
                      >
                        <i>semantics:{{ table.semantics }}</i> <br>
                      </span>
                    </h3>
                    <small v-if="table.description">
                      <i>Description: {{ table.description }}</i></small>
                  </td>
                  <td><a href=".">back to top</a></td>
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
                  <th scope="col">
                    name
                  </th>
                  <th scope="col">
                    type
                  </th>
                  <th scope="col">
                    key
                  </th>
                  <th scope="col">
                    description
                  </th>
                  <th scope="col">
                    semantics
                  </th>
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
                    <span v-if="column.refTable">({{ column.refTable }})</span>&nbsp;
                    <span v-if="column.required">required&nbsp;</span>
                    <span v-if="column.refLink">
                      refLink({{ column.refLink }})&nbsp;
                    </span>
                    <span v-if="column.mappedBy">
                      mappedBy({{ column.mappedBy }})&nbsp;
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

<script>
import ColumnDropModal from './ColumnDropModal.vue'
import ColumnEditModal from './ColumnEditModal.vue'
import {request} from 'graphql-request'
import TableDropModal from './TableDropModal.vue'
import TableEditModal from './TableEditModal.vue'
import Yuml from './Yuml.vue'

import {
  IconAction,
  IconBar,
  IconDanger,
  MessageError,
  Spinner,
} from '@/components/ui/index.js'

export default {
  components: {
    ColumnDropModal,
    ColumnEditModal,
    IconAction,
    IconBar,
    IconDanger,
    MessageError,
    Spinner,
    TableDropModal,
    TableEditModal,
    Yuml,
  },
  props: {
    session: Object,
  },
  data: function() {
    return {
      columnAdd: false,
      columnAlter: false,
      columnDrop: false,
      currentColumn: null,
      currentTable: null,
      graphqlError: null,
      imgFullscreen: false,
      loading: false,
      loadingYuml: false,
      schema: null,
      showAttributes: [],
      tableAdd: false,
      tableAlter: false,
      tableDrop: false,
      tables: null,
    }
  },
  computed: {
    canEdit() {
      return (
        this.session != null &&
        (this.session.email == 'admin' ||
          (this.session.roles &&
            (this.session.roles.includes('Editor') ||
              this.session.roles.includes('Manager'))))
      )
    },
    count() {
      if (this.tables)
        return this.tables.filter((t) => t.externalSchema == undefined).length
      return 0
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        this.loadSchema()
      },
    },
    tableAdd() {
      // eslint-disable-next-line no-console
      console.log('tableadd changed ' + JSON.stringify(this.tableAdd))
    },
    tableAlter() {
      // eslint-disable-next-line no-console
      console.log('tableAlter changed ' + JSON.stringify(this.tableAlter))
    },

  },
  created() {
    this.loadSchema()
  },
  methods: {
    // alter(column) {},
    // drop(column) {},
    loadSchema() {
      this.graphqlError = null
      this.loading = true
      this.loading = true
      this.schema = null
      this.tables = null
      request(
        'graphql',
        '{_schema{name,tables{name,inherit,externalSchema,description,semantics,columns{name,columnType,columnFormat,inherited,key,refSchema,refTable,refLink,mappedBy,required,description,semantics,validationExpression,visibleExpression}}}}',
      )
        .then((data) => {
          this.schema = data._schema.name
          this.tables = data._schema.tables
        })
        .catch((graphqlError) => {
          this.graphqlError = graphqlError.response.errors[0].message
          if (
            this.graphqlError.includes(
              'Field \'_schema\' in type \'Query\' is undefined',
            )
          ) {
            this.graphqlError =
              'Schema is unknown or permission denied (might you need to login with authorized user?)'
          }
        })
        .finally((this.loading = false))
    },
  },
}
</script>

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
  background: rgb(250, 250, 250);
  height: 100%;
  left: 0;
  overflow: auto;
  padding: 10px;
  position: fixed;
  top: 0;
  width: 100%;
}

.fullscreen-icon {
  float: right;
  right: 0px;
  top: 0px;
}
</style>
