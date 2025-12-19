<template>
  <InputString
    v-if="
      ['STRING', 'AUTO_ID'].includes(typeUpperCase) &&
      (typeof modelValue === 'string' ||
        modelValue === undefined ||
        modelValue === null)
    "
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
    v-else-if="
      'EMAIL' === typeUpperCase &&
      (typeof modelValue === 'string' ||
        modelValue === undefined ||
        modelValue === null)
    "
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
    v-else-if="
      NON_REF_ARRAY_TYPES.includes(typeUpperCase) &&
      (Array.isArray(modelValue) || modelValue === undefined)
    "
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
    v-else-if="
      'HYPERLINK' === typeUpperCase &&
      (typeof modelValue === 'string' ||
        modelValue === undefined ||
        modelValue === null)
    "
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
    v-else-if="
      'DECIMAL' === typeUpperCase &&
      (typeof modelValue === 'number' || modelValue === undefined)
    "
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
    v-else-if="
      'INT' === typeUpperCase &&
      (typeof modelValue === 'number' || modelValue === undefined)
    "
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
    v-else-if="
      'LONG' === typeUpperCase &&
      (typeof modelValue === 'string' || modelValue === undefined)
    "
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
    v-else-if="
      ['BOOL'].includes(typeUpperCase) &&
      (typeof modelValue === 'boolean' || modelValue === undefined)
    "
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
    v-else-if="
      ['TEXT'].includes(typeUpperCase) &&
      (typeof modelValue === 'string' || modelValue === undefined)
    "
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
    v-else-if="
      ['CHECKBOX'].includes(typeUpperCase) &&
      options &&
      (Array.isArray(modelValue) || modelValue === undefined)
    "
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
    v-else-if="
      ['REF', 'RADIO'].includes(typeUpperCase) &&
      (typeof modelValue === 'object' || modelValue === undefined) &&
      modelValue !== null &&
      !Array.isArray(modelValue) &&
      refSchemaId &&
      refTableId &&
      refLabel
    "
    v-model="modelValue"
    :limit="20"
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
    v-else-if="
      ['REF_ARRAY', 'CHECKBOX'].includes(typeUpperCase) &&
      ((Array.isArray(modelValue) && isColumnValueObjectArray(modelValue)) ||
        isColumnValueObject(modelValue) ||
        modelValue === undefined) &&
      refSchemaId &&
      refTableId &&
      refLabel
    "
    v-model="modelValue"
    :limit="20"
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
  <InputRef
    v-else-if="
      'SELECT' === typeUpperCase &&
      ((Array.isArray(modelValue) && isColumnValueObjectArray(modelValue)) ||
        isColumnValueObject(modelValue) ||
        modelValue === undefined) &&
      refSchemaId &&
      refTableId &&
      refLabel
    "
    v-model="modelValue"
    :limit="0"
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
  <InputRef
    v-else-if="
      'MULTISELECT' === typeUpperCase &&
      ((Array.isArray(modelValue) && isColumnValueObjectArray(modelValue)) ||
        isColumnValueObject(modelValue) ||
        modelValue === undefined) &&
      refSchemaId &&
      refTableId &&
      refLabel
    "
    v-model="modelValue"
    :multiselect="true"
    :id="id"
    :limit="0"
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
    v-else-if="
      ['REFBACK'].includes(typeUpperCase) &&
      ((Array.isArray(modelValue) && isColumnValueObjectArray(modelValue)) ||
        modelValue === undefined) &&
      refSchemaId &&
      refTableId &&
      refLabel &&
      refBackColumn
    "
    v-model="modelValue"
    :id="id"
    :refSchemaId="refSchemaId"
    :refTableId="refTableId"
    :refLabel="refLabel"
    :refBackColumn="refBackColumn"
    :refBackPrimaryKey="rowKey"
  />

  <InputOntology
    v-else-if="
      ['ONTOLOGY'].includes(typeUpperCase) && refSchemaId && refTableId
    "
    :modelValue="modelValue && typeof modelValue === 'object' && !Array.isArray(modelValue) ? (modelValue as any)['name'] : undefined"
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
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputOntology
    v-else-if="
      ['ONTOLOGY_ARRAY'].includes(typeUpperCase) &&
      refSchemaId &&
      refTableId &&
      (Array.isArray(modelValue) || modelValue === undefined)
    "
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
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputFile
    v-else-if="
      ['FILE'].includes(typeUpperCase) &&
      (isFileValue(modelValue) ||
        modelValue === undefined ||
        modelValue === null)
    "
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
    v-else-if="
      'DATE' === typeUpperCase &&
      (typeof modelValue === 'string' || modelValue === undefined)
    "
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
    v-else-if="
      'DATETIME' === typeUpperCase &&
      (typeof modelValue === 'string' || modelValue === undefined)
    "
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
import { computed } from "vue";
import {
  isColumnValueObject,
  isColumnValueObjectArray,
  isFileValue,
  type CellValueType,
  type columnValue,
} from "../../../metadata-utils/src/types";
import type { IInputProps, IValueLabel } from "../../types/types";
import { getOntologyArrayValues } from "../utils/typeUtils";
import InputArray from "./input/Array.vue";
import InputBoolean from "./input/Boolean.vue";
import InputCheckboxGroup from "./input/CheckboxGroup.vue";
import InputDate from "./input/Date.vue";
import InputDateTime from "./input/DateTime.vue";
import InputDecimal from "./input/Decimal.vue";
import InputFile from "./input/File.vue";
import InputInt from "./input/Int.vue";
import InputLong from "./input/Long.vue";
import InputOntology from "./input/Ontology.vue";
import InputPlaceHolder from "./input/PlaceHolder.vue";
import InputRadioGroup from "./input/RadioGroup.vue";
import InputRef from "./input/Ref.vue";
import InputRefBack from "./input/RefBack.vue";
import InputString from "./input/String.vue";
import InputTextArea from "./input/TextArea.vue";

const modelValue = defineModel<columnValue | columnValue[]>();
const props = withDefaults(
  defineProps<
    IInputProps & {
      type: CellValueType;
      describedBy?: string;
      refSchemaId?: string;
      refTableId?: string;
      refLabel?: string;
      refBackColumn?: string;
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
</script>
