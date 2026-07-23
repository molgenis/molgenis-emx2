<script lang="ts" setup>
import { computed, ref } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import ContentBlockModal from "../../../../tailwind-components/app/components/content/ContentBlockModal.vue";
import FormError from "../../../../tailwind-components/app/components/form/Error.vue";
import { useCartStore } from "../../stores/useCartStore";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import type { ICartItem } from "../../../interfaces/types";

const typeLabels: Record<ICartItem["type"], string> = {
  resource: "collection",
  variable: "variable",
};

const cartStore = useCartStore();

withDefaults(
  defineProps<{
    show: boolean;
  }>(),
  {
    show: false,
  }
);

const error = ref("");

const storeError = computed(
  () =>
    `An error occurred while communicating with the ${cartStore.versionText}. Please try again later.`
);

const sections = computed(() => [
  {
    title: "Collections",
    items: cartStore.resourcesInCart,
    emptyText: "No collections selected.",
  },
  {
    title: "Variables",
    items: cartStore.variablesInCart,
    emptyText: "No variables selected.",
  },
]);

const emit = defineEmits<{
  (e: "close"): void;
}>();

function onClose() {
  error.value = "";
  emit("close");
}

async function sendToStore() {
  error.value = "";
  const responseError = await cartStore.doCartRequest();
  if (responseError) {
    error.value = storeError.value;
  }
}
</script>

<template>
  <SideModal
    :show="show"
    :slideInRight="true"
    :fullScreen="false"
    :includeFooter="true"
    buttonAlignment="left"
    @close="onClose"
  >
    <ContentBlockModal title="Shopping cart">
      <template v-if="!cartStore.isEmpty">
        <p class="mb-2">Review your selection</p>
        <section
          v-for="section in sections"
          :key="section.title"
          class="mb-4 last:mb-0"
        >
          <h3 class="mb-2.5 font-bold">{{ section.title }}</h3>
          <ul v-if="section.items.length" class="list-style-none">
            <li
              v-for="item in section.items"
              :key="item.id"
              class="border-b last:border-none"
            >
              <div class="flex items-center justify-between gap-2 py-0.5">
                <span class="min-w-0 truncate">{{ item.label }}</span>
                <button
                  type="button"
                  class="shrink-0 flex items-center justify-center w-6 h-6 text-button-remove hover:text-blue-800 transition-colors"
                  :aria-label="`remove ${typeLabels[item.type]} from cart`"
                  @click="cartStore.removeFromCart(item.id)"
                >
                  <BaseIcon name="trash" :width="16" />
                </button>
              </div>
            </li>
          </ul>
          <p v-else class="text-gray-600">{{ section.emptyText }}</p>
        </section>
      </template>
      <p v-else>Cart is empty</p>
      <p v-if="!cartStore.isEmpty && !cartStore.resourcesInCart.length">
        Select at least one collection to send a request.
      </p>
      <FormError v-if="error" :message="error" :showPrevNextButtons="false" />
    </ContentBlockModal>
    <template #footer>
      <Button
        :label="`Request from ${cartStore.versionText}`"
        :disabled="!cartStore.resourcesInCart.length"
        icon="external-link"
        @click="sendToStore"
      />
    </template>
  </SideModal>
</template>
