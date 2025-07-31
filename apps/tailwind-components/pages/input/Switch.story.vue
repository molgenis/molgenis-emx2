<script lang="ts" setup>
import { ref } from "vue";

import { InputSwitch } from "#components";
import { type IInputValue } from "../../../metadata-utils/src/types";

const selection = ref<IInputValue>("compact");
</script>

<template>
  <h2 class="text-heading-2xl">Switch</h2>
  <fieldset>
    <legend>Change the current display</legend>
    <InputSwitch
      id="change-view-switch"
      :options="[
        {
          value: 'normal',
          icon: 'ViewNormal',
        },
        {
          value: 'compact',
          icon: 'ViewCompact',
        },
        {
          value: 'table',
          icon: 'ViewTable',
        },
      ]"
      v-model="selection"
    />
  </fieldset>
  <output class="block w-full mt-2 border py-3 px-2 pl-6">
    <code>Selection: {{ selection }}</code>
  </output>
</template>
