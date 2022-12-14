<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <InputGroup>
      <input
        class="form-control"
        :class="{ 'is-invalid': errorMessage }"
        @click="showSelect = true"
        @focus="showSelect = true"
        :value="
          refLabel ? applyJsTemplate(modelValue) : flattenObject(modelValue)
        "
      />
      <template v-slot:append>
        <button
          v-if="modelValue"
          @click="$emit('update:modelValue', null)"
          class="btn btn-outline-primary"
          type="button"
        >
          <i class="fas fa-fw fa-times"></i>
        </button>
      </template>

      <LayoutModal v-if="showSelect" :title="title" @close="showSelect = false">
        <template v-slot:body>
          <TableSearch
            :lookupTableName="tableName"
            :filter="filter"
            :graphqlURL="graphqlURL"
            :canEdit="canEdit"
            @select="select($event)"
            @deselect="deselect(selectIdx)"
          >
            <template v-slot:rowheader="slotProps">
              <ButtonAction @click="select(slotProps.rowkey)">
                Select
              </ButtonAction>
            </template>
          </TableSearch>
        </template>
        <template v-slot:footer>
          <ButtonAlt @click="showSelect = false">Close</ButtonAlt>
        </template>
      </LayoutModal>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import InputGroup from "./InputGroup.vue";
import TableSearch from "../tables/TableSearch.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import FormGroup from "./FormGroup.vue";
import ButtonAlt from "./ButtonAlt.vue";
import ButtonAction from "./ButtonAction.vue";
import { flattenObject } from "../utils";

export default {
  name: "InputRefSelect",
  extends: BaseInput,
  data: function () {
    return {
      showSelect: false,
    };
  },
  components: {
    TableSearch,
    LayoutModal,
    FormGroup,
    ButtonAction,
    ButtonAlt,
    InputGroup,
  },
  props: {
    tableName: String,
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    filter: Object,
    refLabel: String,
    /**
     * if table that this input is selecting from can be edited by the current user
     *  */
    canEdit: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  computed: {
    title() {
      return "Select " + this.tableName;
    },
  },
  methods: {
    flattenObject(objectToFlatten) {
      return objectToFlatten === undefined || objectToFlatten === null
        ? ""
        : flattenObject(objectToFlatten);
    },
    select(event) {
      this.showSelect = false;
      this.$emit("update:modelValue", event);
    },
    applyJsTemplate(object) {
      if (object === undefined || object === null) {
        return "";
      }
      const names = Object.keys(object);
      const vals = Object.values(object);
      try {
        return new Function(...names, "return `" + this.refLabel + "`;")(
          ...vals
        );
      } catch (err) {
        return (
          err.message +
          " we got keys:" +
          JSON.stringify(names) +
          " vals:" +
          JSON.stringify(vals) +
          " and template: " +
          this.refLabel
        );
      }
    },
  },
};
</script>

<docs>

<template>
<div>
  <div>
  <label for="input-ref-select-1">Example </label>
    <InputRefSelect
      id="input-ref-select-1"
      v-model="value1"
      tableName="Pet"
      graphqlURL="/pet store/graphql
    "/>
    Selection: {{ value1 }}
  </div>

  <label for="input-ref-select-2" class="mt-3">Example with default value</label>
  <div>
    <InputRefSelect
        id="input-ref-select-2"
        v-model="value2"
        tableName="Pet"
        graphqlURL="/pet store/graphql"
    />
    Selection: {{ value2 }}
  </div>

  <label for="input-ref-select-3" class="mt-3">Example with filter (category.name = dog)</label>
  <div>
    <InputRefSelect
        id="input-ref-select-3"
        v-model="value3"
        tableName="Pet"
        :filter="{category:{name: {equals:'dog'}}}"
        graphqlURL="/pet store/graphql"
    />
    Selection: {{ value3 }}
  </div>


</div>


</template>

<script>
  export default {
    data: function () {
      return {
        value1: null,
        value2: {name: 'spike'},
        value3: {name: 'pooky'}
      };
    }
  };
</script>

</docs>
