<script setup lang="ts">
import { IServices } from "../../interfaces/directory";
import CardItem from "../CardItem.vue";
import Service from "./Service.vue";

withDefaults(
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
  <div>
    <div v-if="services.length > 1" class="pl-2 pt-2 d-flex">
      <h6>{{ services.length }} services available</h6>
    </div>

    <CardItem v-for="(service, index) in services">
      <Service
        :service="service"
        :is-selected="selectedServices?.includes(service.id)"
        @update:is-added="() => addService(service.id)"
        @update:is-removed="() => removeService(service.id)"
      />
      <hr v-if="index != services.length - 1" />
      <div v-else class="pb-3"></div>
    </CardItem>
  </div>
</template>
