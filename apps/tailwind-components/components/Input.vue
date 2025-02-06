<template>
  <LazyInputString
    v-if="['STRING', 'AUTO_ID', 'LONG', 'INT'].includes(typeUpperCase)"
    :id="id"
    v-model="modelValue as string | number | undefined"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputString
    v-else-if="'EMAIL' === typeUpperCase"
    :id="id"
    v-model="modelValue as string"
    type="email"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input an email address"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputString
    v-else-if="'HYPERLINK' === typeUpperCase"
    :id="id"
    v-model="modelValue as string"
    type="text"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    placeholder="Input a hyperlink"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputBoolean
    v-else-if="['BOOL'].includes(typeUpperCase)"
    :id="id"
    v-model="modelValue as boolean"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :trueLabel="trueLabel"
    :falseLabel="falseLabel"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputTextArea
    v-else-if="['TEXT'].includes(typeUpperCase)"
    v-model="modelValue as string | undefined"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputRadioGroup
    v-else-if="['RADIO'].includes(typeUpperCase)"
    v-model="modelValue as columnValue"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options as IValueLabel[]"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputCheckboxGroup
    v-else-if="['CHECKBOX'].includes(typeUpperCase)"
    v-model="modelValue as columnValue[]"
    :id="id"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options as IValueLabel[]"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputRef
    v-else-if="['REF', 'ONTOLOGY'].includes(typeUpperCase)"
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
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="false"
  />
  <InputRef
    v-else-if="['REF_ARRAY', 'ONTOLOGY_ARRAY'].includes(typeUpperCase)"
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
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
    :is-array="true"
  />
  <LazyInputPlaceHolder v-else :type="typeUpperCase as string" />
</template>

<script setup lang="ts">
import type { IInputProps, IValueLabel } from "~/types/types";
import type {
  CellValueType,
  columnValue,
  columnValueObject,
} from "../../metadata-utils/src/types";
const modelValue = defineModel<columnValue>();
const props = defineProps<
  IInputProps & {
    type: CellValueType;
    describedBy?: string;
    refSchemaId?: string;
    refTableId?: string;
    refLabel?: string;
    options?: IValueLabel[];
    trueLabel?: string;
    falseLabel?: string;
  }
>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);
const typeUpperCase = computed(() => props.type.toUpperCase());
</script>
