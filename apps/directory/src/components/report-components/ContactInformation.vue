<template>
  <div class="mg-report-details-list mb-3">
    <h5>Contact:</h5>
    <div v-if="infoMap.name">{{ infoMap.name.value }}</div>
    <div v-if="contactInformation.email">
      <a :href="'mailto:' + contactInformation.email">
        <i class="fa fa-fw fa-paper-plane" aria-hidden="true" />
        <span class="mg-icon-text">{{ uiText["email"] }}</span>
      </a>
    </div>
    <div v-if="contactInformation.address">
      {{ contactInformation.address }}
    </div>
    <div v-if="website">
      <a :href="website" target="_blank" rel="noopener noreferrer">
        <i class="fa fa-fw fa-globe" aria-hidden="true" />
        <span class="mg-icon-text">{{ website }}</span>
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { mapContactInfo } from "../../functions/viewmodelMapper";
import { useSettingsStore } from "../../stores/settingsStore";

const settingsStore = useSettingsStore();

const { contactInformation } = defineProps<{
  contactInformation: IContactInformation;
  website?: string;
}>();

const uiText = computed(() => settingsStore.uiText);

const infoMap = computed(() => {
  return mapContactInfo({ contact: contactInformation });
});

interface IContactInformation {
  title_before_name: string;
  first_name: string;
  last_name: string;
  email: string;
  role: string;
  address: string;
  country: { name: string; label: string };
}
</script>
