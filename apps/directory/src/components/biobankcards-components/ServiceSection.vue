<template>
  <div>
    <CardItem v-if="services.length > 1">
      <div class="d-flex">
        <router-link
          :to="'/service/'"
          title="Service details"
          class="text-dark"
        >
          <h5 class="font-weight-light">
            {{ services.length }} collections available
          </h5>
        </router-link>
        <div class="ml-auto">
          <CheckBox
            id="ds"
            :is-checked="isAllSelected"
            @change="handleSelectAll"
          />
        </div>
      </div>
    </CardItem>

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
