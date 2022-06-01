<template>
  <FormGroup :id="id" :label="label" :description="description">
    <input type="hidden" class="form-control" />
    <select
      class="form-control"
      :id="id"
      @change.stop.prevent="showSelect = true"
      @click.stop.prevent="showSelect = true"
    >
      <option :value="value" hidden>
        {{ refLabel ? applyJsTemplate(value) : flattenObject(value) }}
      </option>
    </select>

    <LayoutModal
      v-if="showSelect"
      :title="title"
      @close="showSelect = false"
    >
      <template v-slot:body>
        <TableSearch
          :lookupTableName="tableName"
          :filter="filter"
          :graphqlURL="graphqlURL"
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
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
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
  },
  props: {
    tableName: String,
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    filter: Object,
    refLabel: String,
  },
  computed: {
    title() {
      return "Select " + this.table;
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
      this.$emit("input", event);
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
  <label for="">Example </label>
    <InputRefSelect 
      id="input-ref-select-1" 
      v-model="value1" 
      tableName="Pet" 
      graphqlURL="/pet store/graphql
    "/>
    Selection: {{ value1 }}
  </div>

  <label for="">Example with default value</label>
  <div>
    <InputRefSelect
        id="input-ref-select-2"
        v-model="value2"
        tableName="Pet"
        graphqlURL="/pet store/graphql"
    />
    Selection: {{ value2 }}
  </div>

  <div>
    <InputRefSelect
        id="input-ref-select-3"
        v-model="value2"
        tableName="Pet"
        :filter="{category:{name: {equals:'dog'}}}"
        graphqlURL="/pet store/graphql"
    />
    Selection: {{ value2 }}
  </div>
  

</div>

  
</template>

<script>
  export default {
    data: function () {
      return {
        value1: null,
        value2: {name: 'spike'}
      };
    }
  };
</script>

</docs>
