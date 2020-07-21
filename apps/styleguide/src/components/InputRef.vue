<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(el, idx) in arrayValue"
      :key="idx"
      v-bind="$props"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      :showClear="showClear(idx)"
      @add="addRow"
    >
      <select
        :id="id"
        @click="openSelect(idx)"
        :class="{ 'form-control': true, 'is-invalid': error }"
      >
        <option
          v-if="arrayValue[idx] && !showSelect"
          :value="arrayValue[idx]"
          selected
        >
          {{ Object.values(arrayValue[idx]).join(" ") }}
        </option>
      </select>
    </InputAppend>
    <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
      <template v-slot:body>
        <MessageError v-if="error">{{ error }}</MessageError>
        <TableSearch
          :schema="schema"
          :table="refTable"
          :defaultValue="[arrayValue[selectIdx]]"
          @select="select($event, selectIdx)"
          @deselect="deselect(selectIdx)"
        />
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="closeSelect">Close</ButtonAlt>
      </template>
    </LayoutModal>
  </FormGroup>
</template>

<script>
import _baseInput from "./_baseInput";
import TableSearch from "./TableSearch";
import LayoutModal from "./LayoutModal";
import MessageError from "./MessageError";
import FormGroup from "./_formGroup";
import ButtonAlt from "./ButtonAlt";
import InputAppend from "./_inputAppend";

export default {
  extends: _baseInput,
  data: function() {
    return {
      showSelect: false,
      selectIdx: null
    };
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
    refTable: String
  },
  computed: {
    title() {
      return "Select " + this.refTable;
    }
  },
  methods: {
    select(event) {
      this.showSelect = false;
      this.arrayValue[this.selectIdx] = event;
      this.emitValue();
    },
    closeSelect() {
      this.showSelect = false;
    },
    openSelect(idx) {
      this.showSelect = true;
      this.selectIdx = idx;
    },
    deselect(idx) {
      this.showSelect = false;
      this.clearValue(idx);
      this.emitValue();
    }
  }
};
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <InputRef v-model="value" schema="pet store" refTable="Pet"/>
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
                    :defaultValue="value"
            />
            Selection: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: {name: 'spike'}
                };
            }
        };
    </script>
    ```
    Example with list
    ```
    <template>
        <div>
            <InputRef :list="true"
                      v-model="value"
                      refTable="Pet"
                      :defaultValue="[{name:'spike'},{name:'pooky'}]"
            />
            Selection: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: ['spike']
                };
            }
        };
    </script>
    ```
</docs>
