<template>
  <SideModal
    :show="show"
    :slideInRight="true"
    :fullScreen="false"
    :includeFooter="true"
    buttonAlignment="left"
    @close="onClose"
  >
    <ContentBlockModal title="Collections">
      <template v-if="Object.keys(cartStore.datasets).length">
        <p class="mb-2">Review selected collections and linked datasets</p>
        <CartModalResourceList />
      </template>
      <p v-else>Cart is empty</p>
      <FormError v-if="error" :message="error" :showPrevNextButtons="false" />
    </ContentBlockModal>
    <template #footer>
      <Button
        :label="`Request from ${cartStore.getVersionText()}`"
        :disabled="!Object.keys(cartStore.datasets).length"
        icon="external-link"
        @click="sendToStore"
      />
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { computed, ref } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import ContentBlockModal from "../../../../tailwind-components/app/components/content/ContentBlockModal.vue";
import FormError from "../../../../tailwind-components/app/components/form/Error.vue";
import { useCartStore } from "../../stores/useCartStore";
import CartModalResourceList from "./ModalResourceList.vue";

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
