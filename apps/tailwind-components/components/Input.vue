<template>
  <InputString
    v-if="['STRING', 'AUTO_ID'].includes(typeUpperCase)"
    :id="id"
    v-model="modelValue as string | number | undefined"
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
    v-model="modelValue as string"
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
    v-model="modelValue as any[]"
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
    v-model="modelValue as string"
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
    v-model="modelValue as string | number | undefined"
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
    v-model="modelValue as string | number | undefined"
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
    v-model="modelValue as string | undefined"
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
    v-model="modelValue as boolean"
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
    v-model="modelValue as string | undefined"
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
    v-model="modelValue as columnValue"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options as IValueLabel[]"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :align="align"
  />
  <InputCheckboxGroup
    v-else-if="['CHECKBOX'].includes(typeUpperCase) && options"
    v-model="modelValue as columnValue[]"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options as IValueLabel[]"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputRef
    v-else-if="['REF', 'RADIO'].includes(typeUpperCase)"
    v-model="modelValue as columnValueObject"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId as string"
    :refTableId="refTableId as string"
    :refLabel="refLabel as string"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputRef
    v-else-if="['REF_ARRAY', 'CHECKBOX'].includes(typeUpperCase)"
    v-model="modelValue as columnValueObject[]"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId as string"
    :refTableId="refTableId as string"
    :refLabel="refLabel as string"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="true"
  />
  <InputRefBack
    v-else-if="['REFBACK'].includes(typeUpperCase)"
    v-model="modelValue as columnValueObject[]"
    :id="id"
    :refSchemaId="refSchemaId as string"
    :refTableId="refTableId as string"
    :refLabel="refLabel as string"
    :refBackColumn="refBackId as string"
    :refBackPrimaryKey="rowKey"
  />

  <InputOntology
    v-else-if="['ONTOLOGY'].includes(typeUpperCase)"
    :modelValue="modelValue as columnValueObject ? (modelValue as columnValueObject)['name'] as string : undefined"
    @update:modelValue="
      $event ? (modelValue = { name: $event }) : (modelValue = undefined)
    "
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :ontologySchemaId="refSchemaId as string"
    :ontologyTableId="refTableId as string"
    :refLabel="refLabel as string"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputOntology
    v-else-if="['ONTOLOGY_ARRAY'].includes(typeUpperCase)"
    :isArray="true"
    :modelValue="Array.isArray(modelValue)? modelValue.filter(value => value).map( value => (value as columnValueObject)['name'] as string) : []"
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
    :ontologySchemaId="refSchemaId as string"
    :ontologyTableId="refTableId as string"
    :refLabel="refLabel as string"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputFile
    v-else-if="['FILE'].includes(typeUpperCase)"
    v-model="modelValue as columnValueObject"
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
    v-model="modelValue as string"
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
    v-model="modelValue as string"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input a date and time"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputPlaceHolder v-else :type="typeUpperCase as string" />
</template>

<script setup lang="ts">
import type { IInputProps, IValueLabel } from "../types/types";
import type {
  CellValueType,
  columnValue,
  columnValueObject,
} from "../../metadata-utils/src/types";
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

const modelValue = defineModel<columnValue | columnValue[]>();
const props = defineProps<
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
  }
>();
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
