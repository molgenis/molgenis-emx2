<script lang="ts" setup>
import { type InputProps, type IValueLabel } from "~/types/types";
import type { CellValueType } from "metadata-utils/src/types";
const modelValue = defineModel<any>();
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
>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);
</script>

<template>
  <div>
    <template v-if="label">
      <label :for="id">
        <span class="text-title font-bold">{{ label }}</span>
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
    <Input
      v-model="modelValue"
      :id="id"
      :type="type"
      :state="state"
      :describedBy="`${id}-description ${id}-input-error`"
      :placeholder="placeholder"
      :options="options"
      :refSchemaId="refSchemaId as string"
      :refTableId="refTableId as string"
      :refLabel="refLabel as string"
      @update:modelValue="emit('update:modelValue', $event)"
      @blur="emit('blur')"
      @focus="emit('focus')"
    />
    <Message v-if="errorMessage" type="invalid" id="`${id}-input-error`">{{
      errorMessage
    }}</Message>
  </div>
</template>
