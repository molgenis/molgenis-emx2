<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx"
      v-bind="$props"
      :show-clear="showClear(idx)"
      :show-plus="showPlus(idx)"
      @add="addRow"
      @clear="clearValue(idx)"
    >
      <input v-if="readonly" class="form-control" type="hidden">
      <select
        :id="id"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
        :disabled="readonly"
        @click="openSelect(idx)"
      >
        <option
          v-if="valueArray[idx] && !showSelect"
          :readonly="readonly"
          selected
          :value="valueArray[idx]"
        >
          {{ flattenObject(el) }}
        </option>
      </select>
    </InputAppend>
    <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
      <template #body>
        <TableSearch
          :filter="filter"
          :graphql-u-r-l="graphqlURL"
          :table="table"
          @deselect="deselect(selectIdx)"
          @select="select($event)"
        >
          <template #colheader="slotProps">
            <RowButtonAdd
              v-if="slotProps.canEdit"
              :graphql-u-r-l="graphqlURL"
              :table="slotProps.table"
              @close="slotProps.reload"
            />
          </template>
          <template #rowheader="slotProps">
            <ButtonAction @click="select(slotProps.rowkey)">
              Select
            </ButtonAction>
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
import ButtonAction from './ButtonAction.vue'
import ButtonAlt from './ButtonAlt.vue'
import FormGroup from './_formGroup.vue'
import InputAppend from './_inputAppend.vue'
import LayoutModal from '../layout/LayoutModal.vue'
import RowButtonAdd from '../tables/RowButtonAdd.vue'
import TableSearch from '../tables/TableSearch.vue'

export default {
  name: 'InputRefSelect',
  components: {
    ButtonAction,
    ButtonAlt,
    FormGroup,
    InputAppend,
    LayoutModal,
    RowButtonAdd,
    TableSearch,
  },
  extends: _baseInput,
  props: {
    filter: Object,
    /** change if graphql URL != 'graphql'*/
    graphqlURL: {
      default: 'graphql',
      type: String,
    },
    table: String,
  },
  data: function() {
    return {
      selectIdx: null,
      showSelect: false,
    }
  },
  computed: {
    title() {
      return 'Select ' + this.table
    },
  },
  methods: {
    closeSelect() {
      this.showSelect = false
    },
    deselect(idx) {
      this.showSelect = false
      this.clearValue(idx)
      this.emitValue()
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
      this.showSelect = false
      this.emitValue(event, this.selectIdx)
    },
  },
}
</script>
