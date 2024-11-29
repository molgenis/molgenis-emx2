<script setup lang="ts">
import { computed } from "vue";
import { IServices } from "../../interfaces/directory";
import CardItem from "../CardItem.vue";
import CheckBox from "../CheckBox.vue";
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

const isAllSelected = computed(
  () =>
    props.services.length > 0 &&
    props.services.length === props.selectedServices.length
);

function handleSelectAll(isChecked: boolean) {
  if (isChecked) {
    emit(
      "update:addServices",
      props.services.map((service) => service.id)
    );
  } else {
    emit(
      "update:removeServices",
      props.services.map((service) => service.id)
    );
  }
}
</script>

<template>
  <div>
    <div v-if="services.length > 1" class="pl-2 pt-2 d-flex">
      <h6>{{ services.length }} services available</h6>
      <div class="ml-auto">
        <CheckBox
          id="all"
          :is-checked="isAllSelected"
          @change="handleSelectAll"
        ></CheckBox>
      </div>
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
