<script setup lang="ts">
import { s } from "vitest/dist/reporters-5f784f42";
import { IServices } from "../../interfaces/directory";
import Service from "./Service.vue";

const props = withDefaults(
  defineProps<{
    services?: IServices[];
    selectedServices?: string[];
  }>(),
  {
    services: () => [],
    selectedServices: () => [],
  }
);

const emit = defineEmits(["update:addServices", "update:removeServices"]);

function addService(serviceId: string) {
  emit("update:addServices", [serviceId]);
}

function removeService(serviceId: string) {
  emit("update:removeServices", [serviceId]);
}
</script>

<template>
  <div class="px-3 py-1">
    <div v-if="services.length > 1" class="pb-2">
      {{ services.length }} services available
    </div>
    <Service
      v-for="service in services"
      :service="service"
      :is-selected="selectedServices?.includes(service.id)"
      @update:is-added="() => addService(service.id)"
      @update:is-removed="() => removeService(service.id)"
    />
  </div>
</template>
