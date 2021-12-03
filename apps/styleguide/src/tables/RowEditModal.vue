<template>
  <div v-if="showLogin">
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="tableMetadata && (pkey == null || value)">
        <span v-for="column in columnsWithoutMeta" :key="column.name">
          <RowFormInput
            v-if="
              (visibleColumns == null || visibleColumns.includes(column.id)) &&
              visible(column.visible) &&
              column.name != 'mg_tableclass' &&
              //if dependent, show only if dependent value is set
              (!column.refLink || value[column.refLink])
            "
            v-model="value[column.id]"
            :label="column.name"
            :description="column.description"
            :columnType="column.columnType"
            :table="column.refTable"
            :filter="refLinkFilters[column.name]"
            :refLabel="column.refLabel"
            :refBack="column.refBack"
            :required="column.required"
            :errorMessage="errorPerColumn[column.name]"
            :readonly="column.readonly || (pkey && column.key == 1 && !clone)"
            :graphqlURL="graphqlURL"
            :refBackType="getRefBackType(column)"
            :pkey="getPkey(value)"
          />
        </span>
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonOutline @click="saveDraft">Save draft</ButtonOutline>
      <ButtonAction @click="save">Save {{ table }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import LayoutForm from "../layout/LayoutForm.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import MessageError from "../forms/MessageError";
import MessageSuccess from "../forms/MessageSuccess";
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonOutline from "../forms/ButtonOutline";
import SigninForm from "../layout/MolgenisSignin";
import TableMixin from "../mixins/TableMixin";
import GraphqlRequestMixin from "../mixins/GraphqlRequestMixin";
import RowFormInput from "./RowFormInput.vue";

export default {
  extends: TableMixin,
  mixins: [GraphqlRequestMixin],
  data: function () {
    return {
      showLogin: false,
      value: {},
      errorPerColumn: {},
      success: null,
    };
  },
  props: {
    /** when updating existing record, this is the primary key value */
    pkey: Object,
    /** when you want to clone instead of update */
    clone: Boolean,
    /** visible columns, useful if you only want to allow partial edit (array of strings) */
    visibleColumns: Array,
    /** when creating new record, this is initialization value */
    defaultValue: Object,
  },
  components: {
    LayoutForm,
    RowFormInput,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
    SigninForm,
    ButtonOutline,
  },
  methods: {
    getRefBackType(column) {
      if (column.columnType === "REFBACK") {
        //get the other table, find the refback column and check its type
        return this.getTable(column.refTable)
          .columns.filter((c) => c.name === column.refBack)
          .map((c) => c.columnType)[0];
      }
    },
    reload() {
      //override superclass
      if (this.pkey) {
        TableMixin.methods.reload.call(this);
      }
    },
    loginSuccess() {
      this.graphqlError = null;
      this.success = null;
      this.showLogin = false;
    },
    saveDraft() {
      this.executeCommand(true);
    },
    save() {
      this.executeCommand(false);
    },
    executeCommand(isDraft) {
      this.graphqlError = null;
      this.success = null;
      // todo spinner
      let name = this.tableId;

      // indicate if draft
      if (isDraft) {
        this.value["mg_draft"] = true;
      } else {
        this.value["mg_draft"] = false;
      }

      let variables = { value: [this.value] };
      let query = `mutation insert($value:[${name}Input]){insert(${name}:$value){message}}`;
      if (this.pkey && !this.clone) {
        query = `mutation update($value:[${name}Input]){update(${name}:$value){message}}`;
      }
      this.requestMultipart(this.graphqlURL, query, variables)
        .then((data) => {
          if (data.insert) {
            this.success = data.insert.message;
          }
          if (data.update) {
            this.success = data.update.message;
          }
          this.$emit("close");
        })
        .catch((error) => {
          if (error.status === 403) {
            this.graphqlError =
              "Schema doesn't exist or permission denied. Do you need to Sign In?";
            this.showLogin = true;
          } else {
            this.graphqlError = error.errors[0].message;
          }
        });
    },

    eval(expression) {
      try {
        return eval("(function (row) { " + expression + "})")(this.value); // eslint-disable-line
      } catch (e) {
        return "Script error contact admin: " + e.message;
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
      if (this.tableMetadata) {
        this.tableMetadata.columns.forEach((column) => {
          // make really empty if empty
          if (/^\s*$/.test(this.value[column.id])) {
            //this.value[column.name] = null;
          }
          delete this.errorPerColumn[column.id];
          // when required
          if (
            column.required &&
            (this.value[column.id] == null ||
              (typeof this.value[column.id] === "number" &&
                isNaN(this.value[column.id])))
          ) {
            this.errorPerColumn[column.id] = column.name + " is required ";
          } else {
            // when not empty
            // when validation
            if (
              typeof this.value[column.id] !== "undefined" &&
              typeof column.validation !== "undefined"
            ) {
              let value = this.value[column.id]; //used for eval, two lines below
              this.errorPerColumn[column.id] = value; //dummy assign
              this.errorPerColumn[column.id] = this.eval(column.validation);
            } else if (
              column.refLink &&
              this.value[column.id] &&
              this.value[column.refLink.replace(" ", "_")] &&
              !JSON.stringify(this.value[column.id]).includes(
                JSON.stringify(this.value[column.refLink.replace(" ", "_")])
              )
            ) {
              //reflinks should overlap
              this.errorPerColumn[column.id] =
                "value should match your selection in column '" +
                column.refLink +
                "' ";
            }
          }
        });
      }
    },
  },
  computed: {
    columnsWithoutMeta() {
      return this.tableMetadata.columns.filter(
        (c) => !c.name.startsWith("mg_")
      );
    },
    refLinkFilters() {
      let filter = {};
      if (this.tableMetadata) {
        this.tableMetadata.columns.forEach((c) => {
          if (c.refLink) {
            //get the overlap, should be a key column of [refLink][refTable]
            this.tableMetadata.columns.forEach((c2) => {
              if (c2.name === c.refLink) {
                this.schema.tables.forEach((t) => {
                  if (t.name === c.refTable) {
                    t.columns.forEach((c3) => {
                      if (c3.refTable === c2.refTable) {
                        filter[c.name] = {};
                        filter[c.name][c3.name] = {
                          equals: this.value[c.refLink],
                        };
                      }
                    });
                  }
                });
              }
            });
          }
        });
      }
      return filter;
    },
    //@overide
    graphqlFilter() {
      let result = {};
      if (this.tableMetadata && this.pkey) {
        this.tableMetadata.columns
          .filter((c) => c.key == 1)
          .map((c) => (result[c.id] = { equals: this.pkey[c.id] }));
      }
      return result;
    },
    // override from tableMixin
    title() {
      if (this.pkey && this.clone) {
        return `copy ${this.table}`;
      } else if (this.pkey) {
        return `update ${this.table}`;
      } else {
        return `insert ${this.table}`;
      }
    },
  },
  watch: {
    data(val) {
      //TODO prevent loading of parent class if no pkey
      if (this.pkey && val && val.length > 0) {
        let data = val[0];
        let defaultValue = {};
        this.tableMetadata.columns.forEach((column) => {
          //skip key in case of clone and visible
          if (
            data[column.id] &&
            (!this.clone ||
              column.key != 1 ||
              !this.visibleColumns.includes(column.name))
          ) {
            defaultValue[column.id] = data[column.id];
          }
        });
        this.value = defaultValue;
      }
    },
    // validation happens here
    value: {
      handler() {
        this.validate();
      },
      deep: true,
    },
    tableMetadata: {
      handler() {
        this.validate();
      },
      deep: true,
    },
  },
  created() {
    //pass by value
    if (this.defaultValue) {
      this.value = JSON.parse(JSON.stringify(this.defaultValue));
    }
    this.validate();
  },
};
</script>
