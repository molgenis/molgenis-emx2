<template>
  <div :id="id">
    <fieldset>
      <legend>
        <span class="block text-heading-base font-semibold">{{ title }}</span>
        <slot name="description"></slot>
      </legend>
      <div v-if="data.length" class="mt-2 [&>div]:mb-2">
        <InputOption
          v-for="row in data"
          :id="`${id}-${row[row_id]}`"
          :type="type"
          :label="row[row_label]"
          :name="name"
          :value="row_value !== undefined ? row[row_value] : ''"
          :checked="row_checked !== undefined ? row[row_checked] : false"
        >
          <template v-slot:description v-if="row_description">
            <span>{{ row[row_description] }}</span>
          </template>
        </InputOption>
      </div>
    </fieldset>
  </div>
</template>

<script setup lang="ts">
import InputOption from "./Checkbox.vue";

interface Props {
  id: string,
  title: string,
  type: 'checkbox' | 'radio',
  name: string,
  data: any[],
  row_id: string,
  row_label: string,
  row_value?: string,
  row_description?: string,
  row_checked?: string,
}

withDefaults(
  defineProps<Props>(),
  {
    type: 'checkbox'
  }
);
</script>
