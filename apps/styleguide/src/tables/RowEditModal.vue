<template>
  <div v-if="showLogin">
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="tableMetadata && (pkey == null || value)">
        {{ tableMetadata }}
        <span v-for="column in columnsWithoutMeta" :key="column.name">
          <RowFormInput
            v-if="showColumn(column)"
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
import LayoutForm from '../layout/LayoutForm.vue';
import LayoutModal from '../layout/LayoutModal.vue';
import MessageError from '../forms/MessageError';
import MessageSuccess from '../forms/MessageSuccess';
import ButtonAction from '../forms/ButtonAction.vue';
import ButtonAlt from '../forms/ButtonAlt.vue';
import ButtonOutline from '../forms/ButtonOutline';
import SigninForm from '../layout/MolgenisSignin';
import TableMixin from '../mixins/TableMixin';
import GraphqlRequestMixin from '../mixins/GraphqlRequestMixin';
import RowFormInput from './RowFormInput.vue';
import Expressions from '@molgenis/expressions';

export default {
  extends: TableMixin,
  mixins: [GraphqlRequestMixin],
  data: function () {
    return {
      showLogin: false,
      value: {},
      errorPerColumn: {},
      success: null
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
    defaultValue: Object
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
    ButtonOutline
  },
  methods: {
    getRefBackType(column) {
      if (column.columnType === 'REFBACK') {
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
      this.executeSaveCommand(true);
    },
    save() {
      this.executeSaveCommand(false);
    },
    executeSaveCommand(isDraft) {
      this.graphqlError = null;
      this.success = null;
      // TODO: add spinner

      this.setDraft(isDraft);

      const variables = {value: [this.value]};
      const query = this.getUpsertQuery();
      this.requestMultipart(this.graphqlURL, query, variables)
        .then((data) => {
          if (data.insert) {
            this.success = data.insert.message;
          }
          if (data.update) {
            this.success = data.update.message;
          }
          this.$emit('close');
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
    setDraft(isDraft) {
      if (isDraft) {
        this.value['mg_draft'] = true;
      } else {
        this.value['mg_draft'] = false;
      }
    },
    getUpsertQuery() {
      const name = this.tableId;
      if (this.pkey && !this.clone) {
        return `mutation update($value:[${name}Input]){update(${name}:$value){message}}`;
      } else {
        return `mutation insert($value:[${name}Input]){insert(${name}:$value){message}}`;
      }
    },
    showColumn(column) {
      const hasRefValue = !column.refLink || this.value[column.refLink];
      const isColumnVisible =
        this.visibleColumns == null || this.visibleColumns.includes(column.id);
      return (
        isColumnVisible &&
        this.visible(column.visible, column.id) &&
        column.name != 'mg_tableclass' &&
        hasRefValue
      );
    },
    visible(expression, columnId) {
      if (expression) {
        try {
          return Expressions.evaluate(expression, this.value);
        } catch (error) {
          this.errorPerColumn[columnId] = `Invalid visibility expression`;
        }
      } else {
        return true;
      }
    },
    validate() {
      if (this.tableMetadata) {
        this.tableMetadata.columns.forEach((column) =>
          this.validateColumn(column)
        );
      }
    },
    validateColumn(column) {
      delete this.errorPerColumn[column.id];
      const isInvalidNumber =
        typeof this.value[column.id] === 'number' &&
        isNaN(this.value[column.id]);
      const isColumnValueInvalid = // how about undefined?
        this.value[column.id] == null || isInvalidNumber;
      if (column.required && isColumnValueInvalid) {
        this.errorPerColumn[column.id] = column.name + ' is required ';
      } else {
        if (this.value[column.id] !== undefined && column.validation) {
          this.evaluateValidationExpression(column);
        } else if (this.isRefLinkWithoutOverlap(column)) {
          this.errorPerColumn[column.id] =
            "value should match your selection in column '" +
            column.refLink +
            "' ";
        }
      }
    },
    evaluateValidationExpression(column) {
      try {
        if (!Expressions.evaluate(column.validation, this.value)) {
          this.errorPerColumn[
            column.id
          ] = `Error evaluating template: ${column.validation}`;
        }
      } catch (error) {
        this.errorPerColumn[column.id] = `Invalid validation expression`;
      }
    },
    isRefLinkWithoutOverlap(column) {
      if (!column.refLink) {
        return false;
      } else {
        const underscoredRefValue = column.refLink.replace(' ', '_');
        return (
          this.value[column.id] &&
          this.value[underscoredRefValue] &&
          !JSON.stringify(this.value[column.id]).includes(
            JSON.stringify(this.value[underscoredRefValue])
          )
        );
      }
    }
  },
  computed: {
    columnsWithoutMeta() {
      return this.tableMetadata.columns.filter(
        (c) => !c.name.startsWith('mg_')
      );
    },
    refLinkFilters() {
      let filter = {};
      if (this.tableMetadata) {
        this.tableMetadata.columns.forEach((column1) => {
          if (column1.refLink) {
            //get the overlap, should be a key column of [refLink][refTable]
            this.tableMetadata.columns.forEach((column2) => {
              if (column2.name === column1.refLink) {
                this.schema.tables.forEach((table) => {
                  if (table.name === column1.refTable) {
                    table.columns.forEach((tableColumn) => {
                      if (tableColumn.refTable === column2.refTable) {
                        filter[column1.name] = {}; //JJ: Should this overwrite if it already exists?
                        filter[column1.name][tableColumn.name] = {
                          equals: this.value[column1.refLink]
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
      if (this.tableMetadata && this.pkey) {
        return this.tableMetadata.columns
          .filter((column) => column.key == 1)
          .reduce((accum, column) => {
            accum[column.id] = {equals: this.pkey[column.id]};
            return accum;
          }, {});
      } else {
        return {};
      }
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
    }
  },
  watch: {
    data(val) {
      // TODO: prevent loading of parent class if no pkey
      if (this.pkey && val && val.length > 0) {
        let data = val[0];
        let defaultValue = {};
        this.tableMetadata.columns.forEach((column) => {
          // skip key in case of clone and visible
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
      deep: true
    },
    tableMetadata: {
      handler() {
        this.validate();
      },
      deep: true
    }
  },
  created() {
    //pass by value
    if (this.defaultValue) {
      this.value = JSON.parse(JSON.stringify(this.defaultValue));
    }
    this.validate();
  }
};
</script>
