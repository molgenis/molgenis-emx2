<script lang="ts" setup>
import { computed, ref } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import ContentBlockModal from "../../../../tailwind-components/app/components/content/ContentBlockModal.vue";
import FormError from "../../../../tailwind-components/app/components/form/Error.vue";
import { useCartStore } from "../../stores/useCartStore";
import IconButton from "../../../../tailwind-components/app/components/button/IconButton.vue";

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
    `An error occurred while communicating with the ${cartStore.getVersionText()}. Please try again later.`
);

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
      <template v-if="!cartStore.isEmpty()">
        <p class="mb-2">Review your selection</p>
        <ul class="list-style-none">
          <li
            v-for="item in cartStore.cartItems"
            :key="item.id"
            class="border-b-[1px] mb-2 last:border-none last:mb-none"
          >
            <div class="flex justify-center items-start">
              <div class="grow mb-2 self-center">
                <span class="block font-bold">{{ item.label }}</span>
              </div>
              <div>
                <IconButton
                  icon="trash"
                  @click="() => cartStore.removeFromCart(item.id)"
                  class="text-button-remove mb-2"
                  :label="`remove ${item.type} from cart`"
                />
              </div>
            </div>
          </li>
        </ul>
      </template>
      <p v-else>Cart is empty</p>
      <p v-if="!cartStore.isEmpty() && !cartStore.resourcesInCart.length">
        Select at least one collection to send a request.
      </p>
      <FormError v-if="error" :message="error" :showPrevNextButtons="false" />
    </ContentBlockModal>
    <template #footer>
      <Button
        :label="`Request from ${cartStore.getVersionText()}`"
        :disabled="!cartStore.resourcesInCart.length"
        icon="external-link"
        @click="sendToStore"
      />
    </template>
  </SideModal>
</template>
