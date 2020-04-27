<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(el, idx) in arrayValue"
      :key="idx"
      v-bind="$props"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
    >
      <select class="custom-select" :id="id" @click="openSelect">
        <option
          v-if="arrayValue[idx] && !showSelect"
          :value="arrayValue[idx]"
          selected
        >
          {{ arrayValue[idx] }}
        </option>
      </select>
      <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
        <template v-slot:body>
          <MessageError v-if="error">{{ error }}</MessageError>
          <TableSearch
            :schema="schema"
            :table="refTable"
            :selectColumn="refColumn"
            :defaultValue="[arrayValue[idx]]"
            @select="select($event, idx)"
            @deselect="deselect(idx)"
          />
        </template>
        <template v-slot:footer>
          <ButtonAlt @click="closeSelect">Close</ButtonAlt>
        </template>
      </LayoutModal>
    </InputAppend>
  </FormGroup>
</template>

<script>
import _baseInput from './_baseInput'
import TableSearch from './TableSearch'
import LayoutModal from './LayoutModal'
import MessageError from './MessageError'
import FormGroup from './_formGroup'
import ButtonAlt from './ButtonAlt'
import InputAppend from './_inputAppend'

export default {
  extends: _baseInput,
  data: function() {
    return {
      showSelect: false
    }
  },
  components: {
    TableSearch,
    MessageError,
    LayoutModal,
    FormGroup,
    ButtonAlt,
    InputAppend
  },
  props: {
    schema: String,
    refTable: String,
    refColumn: String
  },
  computed: {
    title() {
      return 'Select ' + this.refTable
    }
  },
  methods: {
    select(event, idx) {
      this.showSelect = false
      this.arrayValue[idx] = event
      this.emitValue()
    },
    closeSelect() {
      this.showSelect = false
    },
    openSelect() {
      this.showSelect = true
    },
    deselect(idx) {
      this.showSelect = false
      this.clearValue(idx)
      this.emitValue()
    }
  }
}
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <InputRef v-model="value" schema="pet store" refTable="Pet" refColumn="name"/>
            Selection: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: null
                };
            }
        };
    </script>
    ```
    Example with default value
    ```
    <template>
        <div>
            <InputRef
                    v-model="value"
                    schema="pet store"
                    refTable="Pet"
                    refColumn="name"
                    :defaultValue="value"
            />
            Selection: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: 'spike'
                };
            }
        };
    </script>
    ```
</docs>
