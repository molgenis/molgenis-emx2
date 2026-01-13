<template>
  <InputString
    v-if="['STRING', 'AUTO_ID', 'PERIOD'].includes(typeUpperCase)"
    :id="id"
    v-model="modelValue as string"
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
    v-model="modelValue as string[]"
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
    v-model="modelValue as number"
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
    v-else-if="['INT', 'NON_NEGATIVE_INT'].includes(typeUpperCase)"
    :id="id"
    v-model="modelValue as number"
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
    v-model="modelValue as string"
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
    v-model="modelValue as string"
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
    v-model="modelValue as string | number | boolean | columnValueObject | undefined"
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
    v-model="modelValue as string[]"
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
    v-model="modelValue as columnValueObject | undefined"
    :limit="20"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId!"
    :refTableId="refTableId!"
    :refLabel="refLabel!"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputRef
    v-else-if="['REF_ARRAY', 'CHECKBOX'].includes(typeUpperCase)"
    v-model="modelValue as columnValueObject[] | undefined"
    :limit="20"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId!"
    :refTableId="refTableId!"
    :refLabel="refLabel!"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="true"
  />
  <InputRef
    v-else-if="'SELECT' === typeUpperCase"
    v-model="modelValue as columnValueObject | undefined"
    :limit="0"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId!"
    :refTableId="refTableId!"
    :refLabel="refLabel!"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputRef
    v-else-if="'MULTISELECT' === typeUpperCase"
    v-model="modelValue as columnValueObject[] | undefined"
    :multiselect="true"
    :id="id"
    :limit="0"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :refSchemaId="refSchemaId!"
    :refTableId="refTableId!"
    :refLabel="refLabel!"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :align="align"
  />
  <InputRefBack
    v-else-if="['REFBACK'].includes(typeUpperCase)"
    v-model="modelValue as columnValueObject[] | undefined"
    :id="id"
    :refSchemaId="refSchemaId!"
    :refTableId="refTableId!"
    :refLabel="refLabel!"
    :refBackColumn="refBackColumn!"
    :refBackPrimaryKey="rowKey"
  />

  <InputOntology
    v-else-if="['ONTOLOGY'].includes(typeUpperCase)"
    :modelValue="modelValue && typeof modelValue === 'object' && 'name' in modelValue ? (modelValue as Record<string, any>)['name'] : undefined"
    @update:modelValue="
      $event
        ? (modelValue = { name: $event as string })
        : (modelValue = undefined)
    "
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :ontologySchemaId="refSchemaId!"
    :ontologyTableId="refTableId!"
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
    :ontologySchemaId="refSchemaId!"
    :ontologyTableId="refTableId!"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputFile
    v-else-if="['FILE'].includes(typeUpperCase)"
    v-model="modelValue as IFile"
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
    v-model="modelValue as DateValue"
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
    v-model="modelValue as DateValue"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input a date and time"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
</template>

<script setup lang="ts">
import { computed } from "vue";
import type {
  CellValueType,
  columnValue,
  columnValueObject,
  DateValue,
} from "../../../metadata-utils/src/types";
import type { IFile, IInputProps, IValueLabel } from "../../types/types";
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
  "NON_NEGATIVE_INT_ARRAY",
  "LONG_ARRAY",
  "TEXT_ARRAY",
  "UUID_ARRAY",
  "PERIOD_ARRAY",
];
</script>
