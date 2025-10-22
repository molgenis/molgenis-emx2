<template>
  <InputString
    v-if="['STRING', 'AUTO_ID'].includes(typeUpperCase)"
    :id="id"
    v-model="modelValue"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputString
    v-else-if="'EMAIL' === typeUpperCase"
    :id="id"
    v-model="modelValue"
    type="email"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input an email address"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputArray
    v-else-if="NON_REF_ARRAY_TYPES.includes(typeUpperCase)"
    :id="id"
    v-model="modelValue"
    :type="typeUpperCase"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputString
    v-else-if="'HYPERLINK' === typeUpperCase"
    :id="id"
    v-model="modelValue"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input a hyperlink"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputDecimal
    v-else-if="'DECIMAL' === typeUpperCase"
    :id="id"
    v-model="modelValue"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputInt
    v-else-if="'INT' === typeUpperCase"
    :id="id"
    v-model="modelValue"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputLong
    v-else-if="'LONG' === typeUpperCase"
    :id="id"
    v-model="modelValue"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputBoolean
    v-else-if="['BOOL'].includes(typeUpperCase)"
    :id="id"
    v-model="modelValue"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :trueLabel="trueLabel"
    :falseLabel="falseLabel"
    :align="align"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputTextArea
    v-else-if="['TEXT'].includes(typeUpperCase)"
    v-model="modelValue"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputRadioGroup
    v-else-if="['RADIO'].includes(typeUpperCase) && options"
    v-model="modelValue"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :align="align"
  />
  <InputCheckboxGroup
    v-else-if="['CHECKBOX'].includes(typeUpperCase) && options"
    v-model="modelValue"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputRef
    v-else-if="['REF', 'RADIO'].includes(typeUpperCase)"
    v-model="modelValue"
    :limit="50"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId"
    :refTableId="refTableId"
    :refLabel="refLabel"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputRef
    v-else-if="['REF_ARRAY', 'CHECKBOX'].includes(typeUpperCase)"
    v-model="modelValue"
    :limit="50"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId"
    :refTableId="refTableId"
    :refLabel="refLabel"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="true"
  />
  <InputRefSelect
    v-else-if="'SELECT' === typeUpperCase"
    v-model="modelValue"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId"
    :refTableId="refTableId"
    :refLabel="refLabel"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :align="align"
  />
  <InputRefSelect
    v-else-if="'MULTISELECT' === typeUpperCase"
    v-model="modelValue"
    :multiselect="true"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId"
    :refTableId="refTableId"
    :refLabel="refLabel"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :align="align"
  />
  <InputRefBack
    v-else-if="['REFBACK'].includes(typeUpperCase)"
    v-model="modelValue"
    :id="id"
    :refSchemaId="refSchemaId"
    :refTableId="refTableId"
    :refLabel="refLabel"
    :refBackColumn="refBackId"
    :refBackPrimaryKey="rowKey"
  />

  <InputOntology
    v-else-if="['ONTOLOGY'].includes(typeUpperCase)"
    :modelValue="modelValue ? modelValue['name'] : undefined"
    @update:modelValue="
      $event ? (modelValue = { name: $event }) : (modelValue = undefined)
    "
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :ontologySchemaId="refSchemaId"
    :ontologyTableId="refTableId"
    :refLabel="refLabel"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputOntology
    v-else-if="['ONTOLOGY_ARRAY'].includes(typeUpperCase)"
    :isArray="true"
    :modelValue="getOntologyArrayValues(modelValue)"
    @update:modelValue="
      Array.isArray($event)
        ? (modelValue = $event.map((value) => {
            return { name: value };
          }))
        : (modelValue = [])
    "
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :ontologySchemaId="refSchemaId"
    :ontologyTableId="refTableId"
    :refLabel="refLabel"
    :limit="limit"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputFile
    v-else-if="['FILE'].includes(typeUpperCase)"
    v-model="modelValue"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputDate
    v-else-if="'DATE' === typeUpperCase"
    :id="id"
    v-model="modelValue"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input a date"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputDateTime
    v-else-if="'DATETIME' === typeUpperCase"
    :id="id"
    v-model="modelValue"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input a date and time"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputPlaceHolder v-else :type="typeUpperCase" />
</template>

<script setup lang="ts">
import type { IInputProps, IValueLabel } from "../../types/types";
import type {
  CellValueType,
  columnValue,
  columnValueObject,
} from "../../../metadata-utils/src/types";
import { computed } from "vue";
import InputString from "./input/String.vue";
import InputArray from "./input/Array.vue";
import InputDecimal from "./input/Decimal.vue";
import InputInt from "./input/Int.vue";
import InputLong from "./input/Long.vue";
import InputBoolean from "./input/Boolean.vue";
import InputTextArea from "./input/TextArea.vue";
import InputRadioGroup from "./input/RadioGroup.vue";
import InputCheckboxGroup from "./input/CheckboxGroup.vue";
import InputRef from "./input/Ref.vue";
import InputRefBack from "./input/RefBack.vue";
import InputOntology from "./input/Ontology.vue";
import InputFile from "./input/File.vue";
import InputDate from "./input/Date.vue";
import InputDateTime from "./input/DateTime.vue";
import InputPlaceHolder from "./input/PlaceHolder.vue";
import InputRefSelect from "./input/RefSelect.vue";

const modelValue = defineModel<columnValue | columnValue[]>();
const props = withDefaults(
  defineProps<
    IInputProps & {
      type: CellValueType;
      describedBy?: string;
      refSchemaId?: string;
      refTableId?: string;
      refLabel?: string;
      refBackId?: string;
      rowKey?: any;
      options?: IValueLabel[];
      trueLabel?: string;
      falseLabel?: string;
      align?: "horizontal" | "vertical";
      limit?: number;
    }
  >(),
  {
    limit: 25,
  }
);
const emit = defineEmits(["focus", "blur"]);
const typeUpperCase = computed(() => props.type.toUpperCase());

const NON_REF_ARRAY_TYPES = [
  "STRING_ARRAY",
  "BOOL_ARRAY",
  "DATE_ARRAY",
  "DATETIME_ARRAY",
  "DECIMAL_ARRAY",
  "EMAIL_ARRAY",
  "HYPERLINK_ARRAY",
  "INT_ARRAY",
  "LONG_ARRAY",
  "TEXT_ARRAY",
  "UUID_ARRAY",
  "PERIOD_ARRAY",
];

function getOntologyArrayValues(val: any) {
  return Array.isArray(val)
    ? val
        .filter((value: columnValueObject) => value)
        .map((value: columnValueObject) => value["name"])
    : [];
}
</script>
