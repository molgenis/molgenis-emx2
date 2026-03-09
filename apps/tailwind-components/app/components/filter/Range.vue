<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    id: string;
    legend?: string;
    minLabel?: string;
    maxLabel?: string;
  }>(),
  {
    legend: "",
    minLabel: "Min",
    maxLabel: "Max",
  }
);

const modelValue = defineModel<[any, any]>({ default: () => [null, null] });

function updateMin(val: any) {
  modelValue.value = [val ?? null, modelValue.value[1]];
}
function updateMax(val: any) {
  modelValue.value = [modelValue.value[0], val ?? null];
}
</script>

<template>
  <fieldset class="space-y-2">
    <legend v-if="legend" class="sr-only">{{ legend }}</legend>
    <div class="flex items-center gap-2">
      <label
        :for="`${id}-min`"
        class="text-body-xs w-10 cursor-pointer opacity-70"
      >
        {{ minLabel }}
      </label>
      <div class="flex-1">
        <slot
          name="min"
          :value="modelValue[0]"
          :update="updateMin"
          :id="`${id}-min`"
        />
      </div>
    </div>
    <div class="flex items-center gap-2">
      <label
        :for="`${id}-max`"
        class="text-body-xs w-10 cursor-pointer opacity-70"
      >
        {{ maxLabel }}
      </label>
      <div class="flex-1">
        <slot
          name="max"
          :value="modelValue[1]"
          :update="updateMax"
          :id="`${id}-max`"
        />
      </div>
    </div>
  </fieldset>
</template>
