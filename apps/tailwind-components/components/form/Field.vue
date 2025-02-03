<script lang="ts" setup>
import { type InputProps, type IValueLabel } from "~/types/types";
import type { CellValueType } from "metadata-utils/src/types";
const modelValue = defineModel<any>();
const props = withDefaults(
  defineProps<
    InputProps & {
      type: CellValueType;
      label?: string;
      description?: string | null;
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
  <div>
    <template v-if="label">
      <label :for="id"
        ><span class="text-title font-bold">{{ label }}</span>
        <span class="text-disabled text-body-sm ml-3" v-show="required">
          Required
        </span>
      </label>
    </template>
    <p
      :id="`${id}-input-description`"
      v-if="description"
      class="text-input-description text-body-sm"
    >
      {{ description }}
    </p>
    <LazyInputString
      v-if="['STRING', 'AUTO_ID', 'LONG', 'EMAIL', 'INT'].includes(TYPE)"
      :id="id"
      :modelValue="modelValue"
      :state="state"
      :describedBy="`${id}-description ${id}-input-error`"
      :placeholder="placeholder"
      @update:modelValue="emit('update:modelValue', $event)"
      @blur="emit('blur')"
    />
    <LazyInputTextArea
      v-else-if="['TEXT'].includes(TYPE)"
      v-model="modelValue"
      :id="id"
      :state="state"
      :describedBy="`${id}-description ${id}-input-error`"
      :placeholder="placeholder"
      @update:modelValue="emit('update:modelValue', $event)"
      @blur="emit('blur')"
    />
    <LazyInputRadioGroup
      v-else-if="['RADIO'].includes(TYPE)"
      v-model="modelValue"
      :id="id"
      :state="state"
      :describedBy="`${id}-description ${id}-input-error`"
      :placeholder="placeholder"
      :options="options as IValueLabel[]"
      @update:modelValue="emit('update:modelValue', $event)"
      @blur="emit('blur')"
    />
    <LazyInputCheckboxGroup
      v-else-if="['CHECKBOX'].includes(TYPE)"
      v-model="modelValue"
      :id="id"
      :state="state"
      :describedBy="`${id}-description ${id}-input-error`"
      :placeholder="placeholder"
      :options="options as IValueLabel[]"
      @update:modelValue="emit('update:modelValue', $event)"
      @blur="emit('blur')"
    />
    <LazyInputRef
      v-else-if="['REF'].includes(TYPE)"
      v-model="modelValue"
      :id="id"
      :state="state"
      :describedBy="`${id}-description ${id}-input-error`"
      :placeholder="placeholder"
      :refSchemaId="refSchemaId as string"
      :refTableId="refTableId as string"
      :refLabel="refLabel as string"
      @update:modelValue="emit('update:modelValue', $event)"
      @blur="emit('blur')"
    />
    <Message v-if="errorMessage" type="invalid" id="`${id}-input-error`">{{
      errorMessage
    }}</Message>
  </div>
</template>
