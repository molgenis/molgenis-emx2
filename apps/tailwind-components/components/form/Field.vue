<script lang="ts" setup>
import { type IInputProps, type IValueLabel } from "~/types/types";
import type {
  CellValueType,
  columnValue,
} from "../../../metadata-utils/src/types";

const modelValue = defineModel<columnValue>({ required: true });

defineProps<
  IInputProps & {
    type: CellValueType;
    label?: string;
    description?: string | null;
    required?: boolean;
    errorMessage?: string | null;
    refSchemaId?: string;
    refTableId?: string;
    refLabel?: string;
    options?: IValueLabel[];
    trueLabel?: string;
    falseLabel?: string;
  }
>();
const emit = defineEmits(["focus", "blur"]);
</script>

<template>
  <div>
    <template v-if="label">
      <label :for="id">
        <span class="text-title font-bold">{{ label }}</span>
        <span class="text-disabled text-body-sm ml-3" v-if="required">
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
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      :describedBy="`${id}-description ${id}-input-error`"
      :placeholder="placeholder"
      :options="options"
      :refSchemaId="refSchemaId as string"
      :refTableId="refTableId as string"
      :refLabel="refLabel as string"
      :trueLabel="trueLabel"
      :falseLabel="falseLabel"
      @blur="emit('blur')"
      @focus="emit('focus')"
    />
    <Message v-if="errorMessage" invalid id="`${id}-input-error`">
      {{ errorMessage }}
    </Message>
  </div>
</template>
