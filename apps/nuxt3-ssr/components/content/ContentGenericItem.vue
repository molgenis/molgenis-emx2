<script setup lang="ts">
import { ISectionField } from "interfaces/types";

defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <div class="grid md:grid-cols-3 md:gap-2.5">
    <dt class="flex items-start font-bold text-body-base">
      <div class="flex items-center gap-1">
        {{ field.meta?.name }}
      </div>
    </dt>

    <dd class="col-span-2">
      {{ field.value }}
    </dd>
  </div>
</template>
