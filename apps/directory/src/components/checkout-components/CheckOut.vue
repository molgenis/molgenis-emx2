<template>
  <div>
    <button
      class="btn btn-primary"
      @click="showCart = !showCart"
      :disabled="disableButton"
    >
      <span>{{ uiText["request"] }}</span
      ><span class="badge badge-light ml-2"> {{ nItemsInCart }}</span>
    </button>
    <negotiator-selection v-model="showCart" :bookmark="bookmark" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useSettingsStore } from "../../stores/settingsStore";
import NegotiatorSelection from "../popovers/NegotiatorSelection.vue";

const settingsStore = useSettingsStore();
const checkoutStore = useCheckoutStore();

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    bookmark?: boolean;
  }>(),
  {
    disabled: false,
    bookmark: false,
  }
);

const showCart = ref(false);

const uiText = computed(() => settingsStore.uiText);
const nItemsInCart = computed(
  () =>
    checkoutStore.collectionSelectionCount + checkoutStore.serviceSelectionCount
);

const disableButton = computed(
  () => props.disabled || nItemsInCart.value === 0
);
</script>
