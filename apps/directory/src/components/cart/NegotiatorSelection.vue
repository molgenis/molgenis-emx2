<template>
  <SimpleModal :open="modelValue" :bodyClass="'w-50'">
    <template v-if="selectionCount > 0 && !loading">
      <TabsSection
        :tabs="tabs"
        :active-tab="activeTab"
        @update:active-tab="changeTab"
      />
      <div
        class="card mb-3 border"
        v-if="activeTab === 'Collections' && selectionCount > 0"
        v-for="(collections, biobankName) in selectedCollections"
      >
        <div class="card-header font-weight-bold">
          {{ biobankName }}
        </div>

        <CartItem
          v-for="collection in collections"
          :item="collection"
          :isNonCommercial="isNonCommercialCollection(collection.value)"
          @removeItemFromCart="
            removeCollection({ name: biobankName }, collection.value)
          "
        />
      </div>
      <div
        class="card mb-3 border"
        v-if="
          activeTab === 'Services' && Object.keys(selectedServices).length > 0
        "
        v-for="(services, biobankName) in selectedServices"
      >
        <div class="card-header font-weight-bold">
          {{ biobankName }}
        </div>
        <CartItem
          v-for="service in services"
          :item="service"
          @removeItemFromCart="
            checkoutStore.removeServicesFromSelection(
              { name: biobankName },
              [service.value],
              bookmark
            )
          "
        />
      </div>
    </template>

    <template v-else>
      <p class="py-3 pl-1">
        You haven't selected any collections or services yet.
      </p>
    </template>

    <template v-if="errorMessage">
      <div class="alert alert-danger" role="alert">
        {{ errorMessage }}
      </div>
    </template>

    <template v-slot:modal-footer>
      <div class="bg-primary d-flex align-items-center p-2">
        <button class="btn btn-dark mr-auto" @click="removeAll">
          {{ uiText["remove_all"] }}
        </button>
        <div>
          <span class="text-white font-weight-bold d-block">{{
            modalFooterText
          }}</span>
          <span
            class="text-white"
            v-if="!loading && nSelectedNonCommercialCollections > 0"
          >
            <span
              title="Not available for commercial use"
              class="text-white non-commercial mr-1 fa-brands fa-creative-commons-nc-eu"
            ></span>
            {{ nSelectedNonCommercialCollections }} are non-commercial only
          </span>
        </div>
        <div class="ml-auto">
          <button
            class="btn btn-dark mr-2"
            @click="$emit('update:modelValue', false)"
          >
            {{ uiText["close"] }}
          </button>
          <button
            :disabled="selectionCount === 0"
            class="btn btn-secondary ml-auto"
            @click="sendRequest"
          >
            {{ uiText["send_to_negotiator"] }}
          </button>
        </div>
      </div>
    </template>
  </SimpleModal>
</template>

<script setup lang="ts">
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useSettingsStore } from "../../stores/settingsStore";
// @ts-ignore
import { SimpleModal } from "molgenis-components";
import { computed, ref, watch } from "vue";
import { IBiobankIdentifier } from "../../interfaces/interfaces";
import TabsSection from "../biobankcards-components/TabsSection.vue";
import QueryEMX2 from "../../../../molgenis-components/src/queryEmx2/queryEmx2";
import CartItem from "./CartItem.vue";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    bookmark: boolean;
  }>(),
  {
    bookmark: true,
  }
);

// make sure the active tab is always the one with the non empty items
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue) {
      activeTab.value =
        checkoutStore.collectionSelectionCount > 0 ? "Collections" : "Services";
    }
  }
);

type ICardTab = "Collections" | "Services";

const settingsStore = useSettingsStore();
const checkoutStore = useCheckoutStore();
const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

const tabs = computed(() => {
  return {
    Collections: {
      id: "Collections",
      label: `Collections (${checkoutStore.collectionSelectionCount})`,
      active: activeTab.value === "Collections",
      disabled: checkoutStore.collectionSelectionCount === 0,
    },
    Services: {
      id: "Services",
      label: `Services (${checkoutStore.serviceSelectionCount})`,
      active: activeTab.value === "Services",
      disabled: checkoutStore.serviceSelectionCount === 0,
    },
  };
});

const activeTab = ref<ICardTab>(
  checkoutStore.collectionSelectionCount > 0 ? "Collections" : "Services"
);

const commercialAvailableCollections = ref<string[]>([]);

const loading = ref(true);

new QueryEMX2(graphqlEndpoint)
  .table("Collections")
  .select("id")
  .where("commercial_use")
  .equals(true)
  .execute()
  .then((response) => {
    const collectionsResponse = response as { Collections: { id: string }[] };
    if (
      collectionsResponse.Collections &&
      collectionsResponse.Collections.length
    ) {
      commercialAvailableCollections.value =
        collectionsResponse.Collections.map((collection) => collection.id);
      loading.value = false;
    }
  });

const emit = defineEmits(["update:modelValue"]);

const errorMessage = ref(""); // New reactive error state

function changeTab(tab: ICardTab) {
  activeTab.value = tab;
}

function isNonCommercialCollection(collectionId: string) {
  const isCommercial = commercialAvailableCollections.value.find(
    (colId) => colId === collectionId
  );

  return !isCommercial;
}

function removeCollection(biobank: IBiobankIdentifier, collectionId: string) {
  checkoutStore.removeCollectionsFromSelection(
    biobank,
    [collectionId],
    props.bookmark
  );
}

function removeAll() {
  checkoutStore.removeAllFromSelection(props.bookmark);
  emit("update:modelValue", false);
}

async function sendRequest() {
  errorMessage.value = ""; // Reset error message before request
  try {
    await checkoutStore.sendToNegotiator();
    emit("update:modelValue", false);
  } catch (err) {
    console.info("Negotiator is unavailable. Please try again later.");
    errorMessage.value = "Negotiator is unavailable. Please try again later.";
  }
}

const uiText = computed(() => settingsStore.uiText);

const selectionCount = computed(
  () =>
    checkoutStore.collectionSelectionCount + checkoutStore.serviceSelectionCount
);

const modalFooterText = computed(
  () => `${selectionCount.value} item(s) selected`
);

const selectedCollections = computed(() => checkoutStore.selectedCollections);

const selectedServices = computed(() => checkoutStore.selectedServices);

const nSelectedNonCommercialCollections = computed(() => {
  let allCollectionIds: string[] = [];
  for (const biobank in selectedCollections.value) {
    allCollectionIds = allCollectionIds.concat(
      selectedCollections.value[biobank].map((col) => col.value)
    );
  }
  const nonCommercialCollections = allCollectionIds.filter(
    (col: string) => !commercialAvailableCollections.value.includes(col)
  );
  return nonCommercialCollections.length;
});
</script>

<style scoped>
.cart-selection > div:last-child {
  border: none !important;
}
</style>
