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
      <template v-if="Object.keys(datasetStore.datasets).length">
        <p class="mb-2">Review selected collections and linked datasets</p>
        <StoreModalResourceList />
      </template>
      <p v-else>Cart is empty</p>
      <FormError v-if="error" :message="error" :showPrevNextButtons="false" />
    </ContentBlockModal>
    <template #footer>
      <a
        v-on:click="sendToNegotiator"
        class="flex items-center border rounded-input h-14 px-7.5 text-heading-xl tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
      >
        <span>Request from {{ getSendToText() }}</span>
        <BaseIcon name="external-link" :width="24" />
      </a>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import type { IResources } from "~~/interfaces/catalogue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import ContentBlockModal from "../../../../tailwind-components/app/components/content/ContentBlockModal.vue";
import FormError from "../../../../tailwind-components/app/components/form/Error.vue";
import { useDatasetStore } from "../../stores/useDatasetStore";
import StoreModalResourceList from "./ModalResourceList.vue";

const NEGOTIATOR_ERROR =
  "An error occurred while communicating with the Negotiator. Please try again later.";

const datasetStore = useDatasetStore();

withDefaults(
  defineProps<{
    show: boolean;
  }>(),
  {
    show: false,
  }
);

const error = ref("");

const emit = defineEmits<{
  (e: "close"): void;
}>();

function onClose() {
  error.value = "";
  emit("close");
}

function getSendToText() {
  const version = datasetStore.storeVersion;
  switch (version) {
    case "REMS":
      return "REMS";
    case "negotiatorV3":
      return "Negotiator";
    default:
      return "Unknown data store";
  }
}

async function sendToNegotiator() {
  if (!Object.keys(datasetStore.datasets).length) {
    error.value = `Cart is empty, cannot send to ${getSendToText()}`;
    return;
  }

  const version = datasetStore.storeVersion;
  const dataStoreUrl = datasetStore.datasetStoreUrl;

  switch (version) {
    case "REMS":
      window.open(dataStoreUrl, "_blank");
      break;
    case "negotiatorV3":
      doNegotiatorV3Request();
      break;
    default:
      error.value = "Unknown data store version, cannot send to negotiator";
  }
}

async function doNegotiatorV3Request() {
  error.value = "";
  const url = window.location.origin;
  const humanReadable = getHumanReadableString(); //+ createHistoryJournal();
  const resources = toNegotiatorFormat(datasetStore.datasets);
  const payload: Record<string, any> = { url, humanReadable, resources };

  const response = await fetch(datasetStore.datasetStoreUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (response.ok) {
    datasetStore.clearCart();
    const body = await response.json();
    window.location.href = body.redirectUrl;
  } else {
    handleV3Error(response);
  }
}

async function handleV3Error(response: Response) {
  const statusCode = response.status;
  const jsonResponse = await response.json();
  const detail = jsonResponse.detail ? ` Detail: ${jsonResponse.detail}` : "";
  error.value = NEGOTIATOR_ERROR;
  switch (statusCode) {
    case 400:
      console.error(
        `Negotiator responded with code 400, invalid input.${detail}`
      );
      break;
    case 401:
      console.error(
        `Negotiator responded with code 401, not authorised.${detail}`
      );
      break;
    case 404:
      console.error(`Negotiator not found, error code 404.${detail}`);
      break;
    case 413:
      console.error(
        `Negotiator responded with code 413, request too large.${detail}`
      );
      break;
    case 500:
      console.error(
        `Negotiator responded with code 500, internal server error.${detail}`
      );
      break;
    default:
      console.error(
        `An unknown error occurred with the Negotiator. Please try again later.${detail}`
      );
      break;
  }
}

function toNegotiatorFormat(datasets: Record<string, IResources>) {
  return Object.values(datasets).map((dataset) => ({
    id: dataset.pid,
    name: dataset.name,
  }));
}

function getHumanReadableString() {
  return "";
  // const activeFilterNames = Object.keys(filtersStore.filters);

  // if (!activeFilterNames) return;

  // let humanReadableString = "";
  // const additionText = " and ";
  // const humanReadableStart: Record<string, string> = {};

  // /** Get all the filter definitions for current active filters and make a dictionary name: humanreadable */
  // filtersStore.filterFacets
  //   .filter((fd) => activeFilterNames.includes(fd.facetIdentifier))
  //   .forEach((filterDefinition: IFilterDetails) => {
  //     humanReadableStart[filterDefinition.facetIdentifier] =
  //       filterDefinition.negotiatorRequestString;
  //   });

  // for (const [filterName, filterValue] of Object.entries(
  //   filtersStore.filters
  // )) {
  //   if (!filterValue) continue;
  //   if (!Array.isArray(filterValue)) continue;
  //   humanReadableString += humanReadableStart[filterName];

  //   if (filterName === "search") {
  //     humanReadableString += ` ${filterValue}`;
  //   } else {
  //     humanReadableString += ` ${filterValue
  //       .map((fv) => fv.text)
  //       .join(", ")}`;
  //   }
  //   humanReadableString += additionText;
  // }

  // if (humanReadableString === "") return humanReadableString;

  // return humanReadableString.substring(
  //   0,
  //   humanReadableString.length - additionText.length
  // );
}
</script>
