<script setup lang="ts">
import { c } from "vitest/dist/reporters-5f784f42";
import { IServices } from "../../interfaces/directory";

import { defineProps } from "vue";

const props = withDefaults(
  defineProps<{
    service: IServices;
    isSelected?: boolean;
  }>(),
  {
    isSelected: false,
  }
);

const emit = defineEmits(["update:isAdded", "update:isRemoved"]);

function handleCheckBox() {
  console.log("handleCheckBox", props.isSelected);
  props.isSelected ? emit("update:isRemoved") : emit("update:isAdded");
}
</script>
<template>
  <div class="d-flex">
    <router-link
      :to="'/service/' + service.id"
      title="Service details"
      class="text-dark"
    >
      <h5>
        {{ service.name }}
      </h5>
    </router-link>
    <div class="ml-auto">
      <input
        type="checkbox"
        :id="service.id"
        class="add-to-cart"
        @change="handleCheckBox"
        :checked="isSelected"
        hidden
      />
      <label class="btn" :for="service.id">
        <span v-show="!isSelected"
          ><i class="fa-regular fa-lg fa-square"></i
        ></span>
        <span v-show="isSelected"
          ><i class="fa-regular fa-lg fa-check-square"></i
        ></span>
      </label>
    </div>
  </div>
  <div>
    <small>
      <table class="layout-table w-100">
        <tr>
          <th scope="row" class="pr-1 align-top text-nowrap">Type:</th>
          <td>
            <span>{{
              service.serviceTypes.map((type) => type.label).join(", ")
            }}</span>
          </td>
        </tr>
      </table>

      <RouterLink
        :to="'/service/' + service.id"
        title="Service details"
        class="text-info"
        >More details
      </RouterLink>
    </small>
  </div>
</template>
