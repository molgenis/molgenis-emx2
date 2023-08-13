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
        :value="applyJsTemplate(modelValue, refLabel)"
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
            :schemaName="schemaName"
            :canEdit="canEdit"
            @select="select($event)"
            @deselect="deselect(selectIdx)"
          >
            <template v-slot:rowheader="slotProps">
              <ButtonAction @click="select(slotProps.rowKey)">
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
import { applyJsTemplate } from "../utils";

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
    schemaName: {
      type: String,
      required: false,
    },
    filter: Object,
    refLabel: {
      type: String,
      required: true,
    },
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
    applyJsTemplate,
    async select(event) {
      this.showSelect = false;
      this.$emit("update:modelValue", await event);
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
          schemaName="pet store"
          refLabel="${name}"
      />
      Selection: {{ value1 }}
    </div>

    <label for="input-ref-select-2" class="mt-3">Example with default value</label>
    <div>
      <InputRefSelect
          id="input-ref-select-2"
          v-model="value2"
          tableName="Pet"
          schemaName="pet store"
          refLabel="${name}"
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
          schemaName="pet store"
          refLabel="${name}"
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
