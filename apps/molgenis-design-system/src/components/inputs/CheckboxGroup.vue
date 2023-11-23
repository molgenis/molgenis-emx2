<template>
  <div :id="id" class="">
    <fieldset>
      <legend class="">
        <span class="block">{{ title }}</span>
        <span class="block text-gray-600" v-if="description">{{ description }}</span>
      </legend>
      <div v-if="data.length" class="mt-2 [&>div]:mb-2">
        <Checkbox
          v-for="row in data"
          :id="row[data_id]"
          :label="row[data_label]"
          :name="name"
          :value="data_value !== undefined ? row[data_value] : ''"
          :description="data_description !== undefined ? row[data_description] : ''"          
        />
      </div>
    </fieldset>
  </div>
</template>

<script setup lang="ts">
import Checkbox from './Checkbox.vue';

interface Props {
  id: string,
  title: string,
  description?: string,
  name: string,
  data: any[],
  data_id: string,
  data_label: string,
  data_value?: string,
  data_description?: string,
}

defineProps<Props>();

</script>