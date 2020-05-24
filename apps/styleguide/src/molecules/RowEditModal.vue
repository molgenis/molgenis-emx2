<template>
  <div v-if="showLogin">
    <MessageError v-if="error">{{ error }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="metadata && (pkey == null || defaultValue)">
        <span v-for="column in metadata.columns" :key="column.name">
          <RowFormInput
            v-model="value[column.name]"
            :schema="schema"
            :label="column.name"
            :columnType="column.columnType"
            :refTable="column.refTable"
            :refColumn="column.refColumn"
            :nullable="column.nullable"
            :defaultValue="defaultValue ? defaultValue[column.name] : undefined"
            :error="errorPerColumn[column.name]"
            :readonly="column.readonly || (pkey && column.pkey)"
          />
        </span>
      </LayoutForm>
      {{ JSON.stringify(value) }}
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="error">{{ error }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction @click="executeCommand">{{ title }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import LayoutForm from '../components/LayoutForm.vue'
import LayoutModal from '../components/LayoutModal.vue'
import MessageError from '../components/MessageError'
import MessageSuccess from '../components/MessageSuccess'
import ButtonAction from '../components/ButtonAction.vue'
import ButtonAlt from '../components/ButtonAlt.vue'
import SigninForm from './SigninForm'
import TableMixin from '../mixins/TableMixin'
import RowFormInput from './RowFormInput.vue'
import { request } from 'graphql-request'

export default {
  mixins: [TableMixin],
  data: function() {
    return {
      showLogin: false,
      value: {},
      errorPerColumn: {},
      success: null,
      defaultValue: null
    }
  },
  props: {
    /** when updating existing record, this is the primary key value */
    pkey: String
  },
  components: {
    LayoutForm,
    RowFormInput,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
    SigninForm
  },
  methods: {
    loginSuccess() {
      this.error = null
      this.success = null
      this.showLogin = false
    },
    executeCommand() {
      this.error = null
      this.success = null
      // todo spinner
      let name = this.metadata.name
      let variables = { value: [this.value] }
      let query = `mutation insert($value:[${name}Input]){insert(${name}:$value){message}}`
      if (this.pkey) {
        query = `mutation update($value:[${name}Input]){update(${name}:$value){message}}`
      }
      request('graphql', query, variables)
        .then(data => {
          if (data.insert) {
            this.success = data.insert.message
          }
          if (data.update) {
            this.success = data.update.message
          }
          this.pkey = this.value[this.metadata.pkey]
          this.defaultValue = this.value
          this.$emit('close')
        })
        .catch(error => {
          if (error.response.status === 403) {
            this.error =
              "Schema doesn't exist or permission denied. Do you need to Sign In?"
            this.showLogin = true
          } else {
            this.error = error
          }
        })
    },
    validate() {
      if (this.metadata.columns) {
        this.metadata.columns.forEach(column => {
          // make really empty if empty
          if (/^\s*$/.test(this.value[column.name])) {
            delete this.value[column.name]
          }
          delete this.errorPerColumn[column.name]
          // when empty
          if (this.value[column.name] == null) {
            // when required
            if (column.nullable !== true) {
              this.errorPerColumn[column.name] = column.name + ' is required '
            }
          } else {
            // when not empty
            // when validation
            if (
              typeof this.value[column.name] !== 'undefined' &&
              typeof column.validation !== 'undefined'
            ) {
              let value = this.value[column.name] //used for eval, two lines below
              this.errorPerColumn[column.name] = value //dummy assign
              this.errorPerColumn[column.name] = eval(column.validation) // eslint-disable-line
            }
          }
        })
      }
    }
  },
  computed: {
    // override from tableMixin
    graphql() {
      // todo: must become a typed variable in the query?
      return `{${this.table}(filter:{${this.metadata.pkey}:{equals:"${this.pkey}"}}){data_agg{count}data{${this.columnNames}}}}`
    },
    title() {
      if (this.pkey) {
        return `update ${this.metadata.name}`
      } else {
        return `insert ${this.metadata.name}`
      }
    }
  },
  watch: {
    data(val) {
      if (val && val.length > 0) {
        let data = val[0]
        let defaultValue = {}
        this.metadata.columns.forEach(column => {
          if (data[column.name]) {
            if (column.columnType === 'REF') {
              defaultValue[column.name] = data[column.name][column.refColumn]
            } else if (
              column.columnType.endsWith('ARRAY') ||
              ['REF_ARRAY', 'REFBACK'].includes(column.columnType)
            ) {
              defaultValue[column.name] = []
              if (Array.isArray(data[column.name])) {
                data[column.name].forEach(value =>
                  defaultValue[column.name].push(value[column.refColumn])
                )
              }
            } else {
              defaultValue[column.name] = data[column.name]
            }
          }
        })
        this.defaultValue = defaultValue
      }
    },
    // validation happens here
    value: {
      handler() {
        this.validate()
      },
      deep: true
    }
  },
  created() {
    this.validate()
  }
}
</script>

<!--<docs>-->
<!--    Example-->
<!--    ```-->
<!--    <RowEditModal schema="pet store" table="Pet"/>-->

<!--    ```-->
<!--    Example with lazy load based on pkey-->
<!--    ```-->
<!--    <RowEditModal schema="pet store" table="Pet" pkey="spike"/>-->
<!--    ```-->

<!--    Example with default value explicityly set-->
<!--    ```-->
<!--    <template>-->
<!--        <div>-->
<!--            <RowEditModal schema="pet store" table="Pet" v-model="value" :defaultValue="value"/>-->
<!--            {{JSON.stringify(object,null,2)}}-->
<!--        </div>-->
<!--    </template>-->

<!--    <script>-->
<!--        export default {-->
<!--            data: function () {-->
<!--                return {-->
<!--                    value: {-->
<!--                        name: "spike",-->
<!--                        tags: ["red", "green"],-->
<!--                        status: "sold",-->
<!--                        orders: ["2"]-->
<!--                    },-->
<!--                    metadata: {-->
<!--                        name: "Pet",-->
<!--                        pkey: "name",-->
<!--                        columns: [-->
<!--                            {-->
<!--                                name: "name",-->
<!--                                columnType: "STRING",-->
<!--                                pkey: true-->
<!--                            },-->
<!--                            {-->
<!--                                name: "category",-->
<!--                                columnType: "REF",-->
<!--                                refTable: "Category",-->
<!--                                refColumn: "name"-->
<!--                            },-->
<!--                            {-->
<!--                                name: "photoUrls",-->
<!--                                columnType: "STRING_ARRAY"-->
<!--                            },-->
<!--                            {-->
<!--                                name: "status",-->
<!--                                columnType: "STRING"-->
<!--                            },-->
<!--                            {-->
<!--                                name: "tags",-->
<!--                                columnType: "REF_ARRAY",-->
<!--                                refTable: "Tag",-->
<!--                                refColumn: "name"-->
<!--                            },-->
<!--                            {-->
<!--                                name: "weight",-->
<!--                                columnType: "DECIMAL"-->
<!--                            },-->
<!--                            {-->
<!--                                name: "orders",-->
<!--                                columnType: "REFBACK",-->
<!--                                refTable: "Order",-->
<!--                                refColumn: "orderId",-->
<!--                                mappedBy: "pet"-->
<!--                            }-->
<!--                        ]-->
<!--                    }-->
<!--                };-->
<!--            }-->
<!--        };-->
<!--    </script>-->
<!--    ```-->
<!--</docs>-->
