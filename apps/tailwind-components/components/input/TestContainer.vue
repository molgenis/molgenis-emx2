<template>
  <div class="flex flex-row flex-grow">
    <div class="w-2/3 p-4">
      <slot
        name="default"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        :placeholder="placeholder"
        :required="required"
        :multiple="multiple"
        :errorMessage="errorMessage"
        :onFocus="handleFocus"
        :onBlur="handleBlur"
      ></slot>
    </div>
    <div class="w-1/3 p-4 sticky top-0">
      <FieldSet label="input prop settings">
        <FormField
          v-if="showPlaceholder"
          type="STRING"
          id="test-placeholder"
          v-model="placeholder"
          label="Placeholder"
          description="Placeholder of the input, if applicable"
        />
        <FormField
          v-if="showState"
          type="CHECKBOX"
          id="test-state"
          v-model="state"
          label="state"
          description="additional boolean props you can test"
          :options="[
            { value: 'invalid', label: 'invalid' },
            { value: 'valid', label: 'valid' },
            { value: 'disabled', label: 'disabled' },
          ]"
        />
        <FormField
          v-if="showErrorMessage"
          type="STRING"
          label="errorMessage"
          v-model="errorMessage"
          id="test-container-error-message"
          description="Type here an error message to see how that looks"
        />
        <FormField
          v-if="showRequired"
          type="BOOL"
          label="required"
          v-model="required"
          trueLabel="Required is true"
          falseLabel="Required is false"
          id="test-container-required"
          description="set to true to show required tags"
          align="vertical"
        />
        <FormField
          v-if="showMultiple"
          type="BOOL"
          label="multiple"
          v-model="multiple"
          trueLabel="Multiple"
          falseLabel="Singular"
          id="test-container-multiple"
          align="vertical"
          description="set to true to enable multiple selection"
        />
        <slot name="settings"></slot>
        <p v-if="showBlurCount">focusCount = {{ focusCount }}</p>
        <p v-if="showBlurCount">blurCount = {{ blurCount }}</p>
      </FieldSet>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";

defineProps<{
  showState?: boolean;
  showPlaceholder?: boolean;
  showRequired?: boolean;
  showErrorMessage?: boolean;
  showFocusCount?: boolean;
  showBlurCount?: boolean;
  showMultiple?: boolean;
}>();
const placeholder = ref("");
const state = ref([] as string[]);
const errorMessage = ref("");
const required = ref(false);
const focusCount = ref(0);
const blurCount = ref(0);
const multiple = ref(false);
const valid = computed(() => state.value.includes("valid"));
const invalid = computed(() => state.value.includes("invalid"));
const disabled = computed(() => state.value.includes("disabled"));

function handleBlur() {
  blurCount.value++;
}

function handleFocus() {
  focusCount.value++;
}
</script>
