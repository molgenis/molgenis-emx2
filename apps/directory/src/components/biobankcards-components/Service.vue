<script setup lang="ts">
import { IServices } from "../../interfaces/directory";
import CheckBox from "../CheckBox.vue";

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

function handleCheckBox(isChecked: boolean) {
  isChecked ? emit("update:isAdded") : emit("update:isRemoved");
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
      <CheckBox
        :id="service.id"
        :is-checked="isSelected"
        @change="handleCheckBox"
      />
    </div>
  </div>
  <div>
    <small>
      <table class="layout-table w-100">
        <tbody>
          <tr>
            <th scope="row" class="pr-1 align-top text-nowrap">Type:</th>
            <td>
              <span>{{
                service.serviceTypes.map((type) => type.label).join(", ")
              }}</span>
            </td>
          </tr>
        </tbody>
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
