<template>
  <Card
    class="biobank-card"
    :class="[
      {
        'border border-secondary': biobankInSelection,
        'back-side': showCollections,
      },
    ]"
    :tabs="tabs"
    @update:active-tab="changeTab"
  >
    <template #header>
      <HeaderSection
        :biobank="biobank"
        :hasBiobankQuality="hasBiobankQuality"
        :qualityInfo="qualityInfo"
      />
    </template>

    <CollectionsSection
      v-if="tabs['Collections'].active"
      class="px-3 py-1"
      :biobank="biobank"
      :has-active-filters="filtersStore.hasActiveFilters"
    />

    <ServiceSection
      v-else-if="tabs['Services'].active"
      class="px-3 py-1"
      :services="biobank.services"
      :selected-services="selectedServices"
      @update:addServices="handleAddServices"
      @update:remove-services="handleRemoveServices"
    ></ServiceSection>

    <OrganizationSection
      v-else-if="tabs['Organisation'].active"
      class="px-3 py-1"
      :biobank="biobank"
    />
  </Card>
</template>

<script setup lang="ts">
import { getBiobankDetails } from "../../functions/viewmodelMapper";
import { useSettingsStore } from "../../stores/settingsStore";
import { useQualitiesStore } from "../../stores/qualitiesStore";
import { useFiltersStore } from "../../stores/filtersStore";
import HeaderSection from "./HeaderSection.vue";
import OrganizationSection from "./OrganizationSection.vue";
import Card from "../Card.vue";
import { computed, onBeforeMount, ref } from "vue";
import { IBiobanks } from "../../interfaces/directory";
import ServiceSection from "./ServiceSection.vue";
import CollectionsSection from "./CollectionsSection.vue";
import { useCheckoutStore } from "../../stores/checkoutStore";

const settingsStore = useSettingsStore();
const qualitiesStore = useQualitiesStore();
const checkoutStore = useCheckoutStore();
const filtersStore = useFiltersStore();

const props = withDefaults(
  defineProps<{
    biobank: IBiobanks;
    fullSize?: boolean;
  }>(),
  {
    fullSize: false,
  }
);

type IBiobankCardTab = "Collections" | "Services" | "Organisation";
const showCollections = ref(false);
const activeTab = ref<IBiobankCardTab>("Collections");
const tabs = computed(() => {
  return {
    Collections: {
      id: "Collections",
      label: "Collections",
      active: activeTab.value === "Collections",
      disabled: props.biobank?.collections?.length === 0,
    },
    Services: {
      id: "Services",
      label: "Services",
      active: activeTab.value === "Services",
      disabled: props.biobank?.services?.length === 0,
    },
    Organisation: {
      id: "Organisation",
      label: "Organisation",
      active: activeTab.value === "Organisation",
      disabled: false,
    },
  };
});

onBeforeMount(async () => {
  await qualitiesStore.getQualityStandardInformation();
  showCollections.value = settingsStore.config.biobankCardShowCollections;
});

function changeTab(tab: IBiobankCardTab) {
  activeTab.value = tab;
}

const selectedServices = computed(() => {
  return (
    checkoutStore.selectedServices[props.biobank.name]?.map(
      (selectedService) => selectedService.value
    ) ?? []
  );
});

function handleAddServices(selectedServiceIds: string[]) {
  const selectedServices =
    props.biobank.services?.filter((service) =>
      selectedServiceIds.includes(service.id)
    ) ?? [];
  const servicesLabelValuePair = selectedServices.map((service) => {
    return { label: service.name, value: service.id };
  });

  checkoutStore.addServicesToSelection(
    props.biobank,
    servicesLabelValuePair,
    true
  );
}

function handleRemoveServices(selectedServiceIds: string[]) {
  checkoutStore.removeServicesFromSelection(
    props.biobank,
    selectedServiceIds,
    true
  );
}

const qualityInfo = computed(() => {
  const qualityAttributes = biobankcardViewmodel.value.attributes.find(
    (attr) => attr.type === "quality"
  );
  return qualityAttributes.value
    ? qualityAttributes.value.map((quality: Record<string, any>) => {
        return (
          qualitiesStore.qualityStandardsDictionary as Record<string, any>
        )[quality.quality_standard.name];
      })
    : [];
});

const biobankcardViewmodel = computed(() => {
  const { viewmodel } = getBiobankDetails(props.biobank);
  const attributes = [];

  for (const item of settingsStore.config.biobankColumns) {
    if (item.showOnBiobankCard) {
      attributes.push(
        viewmodel.attributes.find(
          (vm: Record<string, any>) => vm.label === item.label
        )
      );
    }
  }
  return { attributes };
});

const hasBiobankQuality = computed(() => {
  return biobankcardViewmodel.value.attributes.some(
    (attr) => attr.type === "quality" && attr.value && attr.value.length
  );
});

const biobankInSelection = computed(() => {
  const biobankIdentifier: string = props.biobank.name;
  return (
    checkoutStore.selectedCollections[biobankIdentifier] !== undefined ||
    checkoutStore.selectedServices[biobankIdentifier] !== undefined
  );
});
</script>

<style scoped>
.collection-icon {
  position: relative;
  top: 0.25em;
  line-height: unset;

  clip-path: inset(-15% 0% 75% 0%);
}

.certificate-icon {
  font-size: 0.8rem;
}
</style>

<style>
.btn-link:focus {
  box-shadow: none;
}

.biobank-section,
.collections-section {
  overflow: auto;
}

.biobank-card {
  width: 25rem;
}

.biobank-card > header,
.collection-header {
  display: flex;
  min-height: 3rem;
  flex-direction: column;
  justify-content: center;
}

article {
  padding: 1.5rem;
}

article footer {
  padding: 1.5rem 0 0 0;
}

article {
  padding: 0;
  position: relative;
  height: 28rem;
}

article {
  box-shadow: 0 6.4px 14.4px 0 rgba(0, 0, 0, 0.132),
    0 1.2px 3.6px 0 rgba(0, 0, 0, 0.108);
}

article section {
  height: 100%;
  width: 100%;
}

.right-content-list {
  list-style-type: none;
  margin-left: -2.5rem;
}

.right-content-list:not(:last-child) {
  margin-bottom: 1.5rem;
}

.right-content-list li {
  margin-bottom: 0.5rem;
}

.popover-content {
  margin-bottom: 15px;
}

.popover-content:last-child {
  margin-bottom: 0;
}

.popover-content .quality-standard-label {
  font-weight: 700;
}

.popover-content .quality-standard-definition {
  font-weight: 600;
}
</style>
