<template>
  <div :id="id">
    <fieldset>
      <legend>
        <span
          :data-required="required"
          class="
            block
            text-heading-base
            font-semibold mb-1
            data-[required='true']:after:content-['*']
            data-[required='true']:after:text-[0.9em]
            data-[required='true']:after:font-bold
            data-[required='true']:after:ml-1
            data-[required='true']:after:text-red-500
          "
        >
          {{ title }}
        </span>
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
import InputOption from "./InputOption.vue";

interface Props {
  id: string,
  title: string,
  type: 'checkbox' | 'radio',
  name: string,
  required?: boolean,
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
    type: 'checkbox',
    required: false
  }
);
</script>
