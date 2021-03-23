<template>
  <FormGroup v-bind="$props">
    <div
      v-for="row in data"
      :key="JSON.stringify(row)"
      class="form-check custom-control custom-checkbox"
    >
      <input
        v-if="list"
        v-model="selection"
        class="form-check-input"
        type="checkbox"
        :value="getPkey(row)"
        @change="$emit('update:modelValue', selection)"
      >
      <input
        v-else
        v-model="selection"
        class="form-check-input"
        :name="id"
        type="radio"
        :value="getPkey(row)"
        @change="$emit('update:modelValue', getPkey(row))"
      >
      <label class="form-check-label">
        {{ flattenObject(getPkey(row)) }}
      </label>
    </div>
    <ButtonAlt class="pl-1" icon="fa fa-search" @click="showSelect = true">
      more
    </ButtonAlt>
    <ButtonAlt class="pl-1" icon="fa fa-clear" @click="emitClear">
      clear
    </ButtonAlt>
    <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
      <template #body>
        <MessageError v-if="errorMessage">
          {{ graphqlError }}
        </MessageError>
        <TableSearch
          v-if="list"
          v-model:selection="selection"
          :filter="filter"
          :graphql-u-r-l="graphqlURL"
          :limit="10"
          :show-select="true"
          :table="table"
        >
          <template name="colheader">
            <RowButtonAdd v-if="canEdit" :table="table" @close="reload" />
          </template>
        </TableSearch>
        <TableSearch
          v-else
          :filter="filter"
          :graphql-u-r-l="graphqlURL"
          :limit="10"
          :selection="[valueArray[0]]"
          :show-select="true"
          :table="table"
          @deselect="emitClear"
          @select="select($event)"
        >
          <template name="colheader">
            <RowButtonAdd v-if="canEdit" :table="table" @close="reload" />
          </template>
        </TableSearch>
      </template>
      <template #footer>
        <ButtonAlt @click="closeSelect">
          Close
        </ButtonAlt>
      </template>
    </LayoutModal>
  </FormGroup>
</template>

<script>
import _baseInput from './_baseInput.vue'
import ButtonAlt from './ButtonAlt.vue'
import FormGroup from './_formGroup.vue'
import LayoutModal from '../layout/LayoutModal.vue'
import MessageError from './MessageError.vue'
import TableMixin from '../mixins/TableMixin.vue'
import TableSearch from '../tables/TableSearch.vue'

export default {
  components: {
    ButtonAlt,
    FormGroup,
    LayoutModal,
    MessageError,
    TableSearch,
  },
  extends: _baseInput,
  mixins: [TableMixin],
  props: {
    filter: Object,
    /** change if graphql URL != 'graphql'*/
    graphqlURL: {
      default: 'graphql',
      type: String,
    },
  },
  emits: ['update:modelValue'],
  data: function() {
    return {
      id: Math.random(),
      options: [],
      selectIdx: null,
      selection: [],
      showSelect: false,
    }
  },
  computed: {
    title() {
      return 'Select ' + this.table
    },
  },
  watch: {
    value() {
      this.selection = this.value ? this.value : []
    },
  },
  created() {
    this.limit = 8
    this.selection = this.value ? this.value : []
    this.reloadMetadata()
  },
  methods: {
    closeSelect() {
      this.showSelect = false
      if (this.list) {
        this.$emit('update:modelValue', this.selection)
      }
    },
    emitClear() {
      if (this.list) this.$emit('update:modelValue', [])
      else this.$emit('update:modelValue', null)
    },
    flattenObject(object) {
      let result = ''
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          // nothing
        } else if (typeof object[key] === 'object') {
          result += this.flattenObject(object[key])
        } else {
          result += ' ' + object[key]
        }
      })
      return result
    },
    openSelect(idx) {
      this.showSelect = true
      this.selectIdx = idx
    },
    select(event) {
      if (this.list) {
        this.$emit('update:modelValue', this.selection)
      } else {
        this.$emit('update:modelValue', event)
      }
    },
  },
}
</script>
