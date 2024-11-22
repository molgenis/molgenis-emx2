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

const emit = defineEmits(["update:selectedServices"]);

function addService(serviceId: string) {
  console.log("add service", serviceId);
  const selected = [props.selectedServices, serviceId].flat();
  emit("update:selectedServices", selected);
}

function removeService(serviceId: string) {
  console.log("remove service", serviceId);
  const selected = props.selectedServices.filter((id) => id !== serviceId);
  emit("update:selectedServices", selected);
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
