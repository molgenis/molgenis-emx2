<script setup lang="ts">
import type { InputProps, IValueLabel } from "~/types/types";
import type { CellValueType } from "metadata-utils/src/types";
const modelValue = defineModel<any>();
const props = withDefaults(
  defineProps<
    InputProps & {
      type: CellValueType;
      label?: string;
      description?: string | null;
      describedBy?: string;
      required?: boolean;
      errorMessage?: string | null;
      refSchemaId?: string;
      refTableId?: string;
      refLabel?: string;
      options?: IValueLabel[];
    }
  >(),
  {
    required: false,
  }
);
const emit = defineEmits(["focus", "blur", "update:modelValue"]);
const TYPE = computed(() => props.type.toUpperCase());
</script>

<template>
  <LazyInputString
    v-if="['STRING', 'AUTO_ID', 'LONG', 'EMAIL', 'INT'].includes(TYPE)"
    :id="id"
    :modelValue="modelValue"
    :state="state"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputTextArea
    v-else-if="['TEXT'].includes(TYPE)"
    v-model="modelValue"
    :id="id"
    :state="state"
    :describedBy="describedBy"
    :placeholder="placeholder"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputRadioGroup
    v-else-if="['RADIO'].includes(TYPE)"
    v-model="modelValue"
    :id="id"
    :state="state"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options as IValueLabel[]"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <LazyInputCheckboxGroup
    v-else-if="['CHECKBOX'].includes(TYPE)"
    v-model="modelValue"
    :id="id"
    :state="state"
    :describedBy="describedBy"
    :placeholder="placeholder"
    :options="options as IValueLabel[]"
    @update:modelValue="emit('update:modelValue', $event)"
    @focus="emit('focus')"
    @blur="emit('blur')"
  />
  <InputRef
    v-else-if="['REF', 'ONTOLOGY'].includes(TYPE)"
    v-model="modelValue"
    :id="id"
    :state="state"
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
    v-else-if="['REF_ARRAY', 'ONTOLOGY_ARRAY'].includes(TYPE)"
    v-model="modelValue"
    :id="id"
    :state="state"
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
  <LazyInputPlaceHolder v-else v-model="modelValue" :type="TYPE" />
</template>
