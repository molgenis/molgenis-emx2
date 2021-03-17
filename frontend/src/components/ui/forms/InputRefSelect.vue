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
import _baseInput from "./_baseInput.vue";
import TableSearch from "../tables/TableSearch.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import FormGroup from "./_formGroup.vue";
import ButtonAlt from "./ButtonAlt.vue";
import InputAppend from "./_inputAppend.vue";
import ButtonAction from "./ButtonAction.vue";
import RowButtonAdd from "../tables/RowButtonAdd.vue";

export default {
  name: "InputRefSelect",
  components: {
    TableSearch,
    LayoutModal,
    FormGroup,
    ButtonAction,
    ButtonAlt,
    InputAppend,
    RowButtonAdd,
  },
  extends: _baseInput,
  props: {
    /** change if graphql URL != 'graphql'*/
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    table: String,
    filter: Object,
  },
  data: function () {
    return {
      showSelect: false,
      selectIdx: null,
    };
  },
  computed: {
    title() {
      return "Select " + this.table;
    },
  },
  methods: {
    select(event) {
      this.showSelect = false;
      this.emitValue(event, this.selectIdx);
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
    },
    flattenObject(object) {
      let result = "";
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += " " + object[key];
        }
      });
      return result;
    },
  },
};
</script>
