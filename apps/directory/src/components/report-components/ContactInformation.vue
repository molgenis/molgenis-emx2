<template>
  <div class="mg-report-details-list mb-3">
    <div class="font-weight-bold mr-1">Contact:</div>
    <div v-if="name">{{ name }}</div>
    <div v-if="contactInformation.email">
      <a
        :href="'mailto:' + contactInformation.email"
        @click="
          trackMatomoEvent({
            category: 'Contact',
            action: 'Email Link Click',
            name: contactInformation.email,
          })
        "
      >
        <i class="fa fa-fw fa-paper-plane" aria-hidden="true" />
        <span class="mg-icon-text">{{ uiText["email"] }}</span>
      </a>
    </div>
    <div v-if="contactInformation.address">
      {{ contactInformation.address }}
    </div>
    <div v-if="contactInformation.zip && contactInformation.city">
      {{ contactInformation.zip }} {{ contactInformation.city }}
    </div>
    <div v-if="contactInformation.country">
      {{ contactInformation.country.label || contactInformation.country.name }}
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
import { getName } from "../../functions/viewmodelMapper";
import { useSettingsStore } from "../../stores/settingsStore";
import { IPersons } from "../../interfaces/directory";
import trackMatomoEvent from "../../functions/trackMatomoEvent";

const settingsStore = useSettingsStore();

const { contactInformation } = defineProps<{
  contactInformation: IPersons;
  website?: string;
}>();

const uiText = computed(() => settingsStore.uiText);
const name = computed(() => getName(contactInformation));
</script>
