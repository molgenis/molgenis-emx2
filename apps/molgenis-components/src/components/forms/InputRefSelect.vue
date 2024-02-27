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
        @click="showSelect = !this.readonly"
        @focus="showSelect = !this.readonly"
        :value="applyJsTemplate(modelValue, refLabel)"
        :readonly="readonly"
      />
      <template v-slot:append>
        <button
          v-if="modelValue && !this.readonly"
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
            :tableId="tableId"
            :filter="filter"
            :schemaId="schemaId"
            :canEdit="canEdit"
            @select="select($event)"
            @deselect="deselect(selectIdx)"
          >
            <template v-slot:rowheader="slotProps">
              <ButtonAction @click="select(slotProps.row)">
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
    tableId: String,
    schemaId: {
      type: String,
      required: false,
    },
    filter: Object,
    refLabel: {
      type: String,
      required: true,
    },
    canEdit: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  computed: {
    title() {
      return "Select " + this.tableId; //todo need a label
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
        tableId="QualityInfoBiobanks"
        schemaId="Directory demo"
        refLabel="${name}"
      />
      Selection: {{ value1 }}
    </div>

    <div>
      <label for="input-ref-select-1">Example readonly </label>
      <InputRefSelect
        id="input-ref-select-1b"
        v-model="value1"
        tableId="Pet"
        schemaId="pet store"
        refLabel="${name}"
        :readonly="true"
      />
      Selection: {{ value1 }}
    </div>

    <label for="input-ref-select-2" class="mt-3">
      Example with default value
    </label>
    <div>
      <InputRefSelect
        id="input-ref-select-2"
        v-model="value2"
        tableId="Pet"
        schemaId="pet store"
        refLabel="${name}"
      />
      Selection: {{ value2 }}
    </div>

    <label for="input-ref-select-3" class="mt-3">
      Example with filter (category.name = dog)
    </label>
    <div>
      <InputRefSelect
        id="input-ref-select-3"
        v-model="value3"
        tableId="Pet"
        :filter="{ category: { name: { equals: 'dog' } } }"
        schemaId="pet store"
        refLabel="${name}"
      />
      Selection: {{ value3 }}
    </div>

    <label for="input-ref-select-4"> Example with different label </label>
    <div>
      <InputRefSelect
        id="input-ref-select-4"
        v-model="value4"
        tableId="Pet"
        schemaId="pet store"
        refLabel="${status}"
      />
      Selection: {{ value4 }}
    </div>
  </div>
</template>

<script>
export default {
  data: function () {
    return {
      value1: null,
      value2: { name: "spike" },
      value3: { name: "pooky" },
      value4: null,
    };
  },
};
</script>
</docs>
