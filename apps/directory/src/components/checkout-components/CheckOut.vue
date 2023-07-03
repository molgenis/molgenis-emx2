<template>
  <div>
    <button
      class="btn btn-primary"
      @click="showCart = !showCart"
      :disabled="disableButton"
    >
      <span>{{ uiText["request"] }}</span
      ><span class="badge badge-light ml-2">
        {{ collectionSelectionCount }}</span
      >
    </button>
    <negotiator-selection v-model="showCart" :bookmark="bookmark" />
  </div>
</template>

<script>
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useSettingsStore } from "../../stores/settingsStore";
import NegotiatorSelection from "../popovers/NegotiatorSelection.vue";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    const checkoutStore = useCheckoutStore();
    return { settingsStore, checkoutStore };
  },
  components: {
    NegotiatorSelection,
  },
  props: {
    disabled: {
      type: Boolean,
      required: false,
      default: () => false,
    },
    bookmark: {
      type: Boolean,
      required: false,
      default: () => true,
    },
  },
  computed: {
    uiText() {
      return this.settingsStore.uiText;
    },
    collectionSelectionCount() {
      return this.checkoutStore.collectionSelectionCount;
    },
    disableButton() {
      return this.disabled || this.collectionSelectionCount === 0;
    },
  },
  data: () => ({
    showCart: false,
  }),
};
</script>
