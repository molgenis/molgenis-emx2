<template>
  <simple-modal :open="modelValue" :bodyClass="'w-50'">
    <template v-if="selectionCount > 0">
      <TabsSection
        :tabs="tabs"
        :active-tab="activeTab"
        @update:active-tab="changeTab"
      />
      <div
        class="card mb-3 border"
        v-for="(collections, biobankName) in selectedCollections"
      >
        <div class="card-header font-weight-bold">
          {{ biobankName }}
        </div>
        <div class="cart-selection">
          <div
            class="card-body d-flex border-bottom"
            :key="`${collection.label}-${index}`"
            v-for="(collection, index) in collections.sort((a, b) =>
              a.label.localeCompare(b.label)
            )"
          >
            <div>
              <span
                v-if="!loading && isNonCommercialCollection(collection.value)"
                class="fa-brands fa-creative-commons-nc-eu text-danger non-commercial mr-2"
                title="Not available for commercial use"
              ></span>

              <span> {{ collection.label }}</span>
            </div>
            <div
              class="pl-3 ml-auto"
              @click="
                removeCollectionsFromSelection({
                  biobank: { name: biobankName },
                  collections: [collection],
                  bookmark: bookmark,
                })
              "
            >
              <span
                class="fa fa-times text-bold remove-collection"
                title="Remove collection"
              ></span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <p class="py-3 pl-1">You haven't selected any collections yet.</p>
    </template>

    <template v-if="errorMessage">
      <div class="alert alert-danger" role="alert">
        {{ errorMessage }}
      </div>
    </template>

    <template v-slot:modal-footer>
      <div class="bg-primary d-flex align-items-center p-2">
        <button class="btn btn-dark mr-auto" @click="removeAllCollections">
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
  </simple-modal>
</template>

<script setup lang="ts">
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useSettingsStore } from "../../stores/settingsStore";
// @ts-ignore
import { SimpleModal } from "molgenis-components";
import { computed, ref } from "vue";
import { IBiobankIdentifier } from "../../interfaces/interfaces";
import TabsSection from "../biobankcards-components/TabsSection.vue";
import QueryEMX2 from "../../../../molgenis-components/src/queryEmx2/queryEmx2";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    bookmark: boolean;
  }>(),
  {
    bookmark: true,
  }
);

type ICardTab = "Collections" | "Services";
const tabs = ["Collections", "Services"];
const activeTab = ref<ICardTab>("Collections");

const settingsStore = useSettingsStore();
const checkoutStore = useCheckoutStore();
const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

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

function removeCollectionsFromSelection(collectionData: {
  biobank: IBiobankIdentifier;
  collections: { label: string; value: string }[];
  bookmark: boolean;
}) {
  checkoutStore.removeCollectionsFromSelection(collectionData);
}

function removeAllCollections() {
  checkoutStore.removeAllCollectionsFromSelection({
    bookmark: props.bookmark,
  });
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
  () => `${selectionCount.value} collection(s) selected`
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
