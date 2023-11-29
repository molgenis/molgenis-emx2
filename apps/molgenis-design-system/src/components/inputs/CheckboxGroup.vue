<template>
  <div :id="id">
    <fieldset>
      <legend>
        <span class="block">{{ title }}</span>
        <span class="bloc" v-if="description">
          {{ description }}
        </span>
      </legend>
      <div v-if="data.length" class="mt-2 [&>div]:mb-2">
        <Checkbox
          v-for="row in data"
          :id="`${id}-${row[row_id]}`"
          :label="row[row_label]"
          :name="name"
          :value="row_value !== undefined ? row[row_value] : ''"
          :description="
            row_description !== undefined ? row[row_description] : ''
          "
          :checked="row_checked !== undefined ? row[row_checked] : false"
        />
      </div>
    </fieldset>
  </div>
</template>

<script setup lang="ts">
import Checkbox from "./Checkbox.vue";

interface Props {
  id: string;
  title: string;
  description?: string;
  name?: string;
  data: any[];
  row_id: string;
  row_label: string;
  row_value?: string;
  row_description?: string;
  row_checked?: string;
}

defineProps<Props>();
</script>
