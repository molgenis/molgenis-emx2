<template>
  <header class="border-0 biobank-card-header p-1">
    <h5 class="pt-1 pl-1 pr-1 mt-1">
      <router-link :to="'/biobank/' + biobank.id" class="text-dark">
        <span class="fa fa-server mr-2 text-primary" aria-hidden="true" />
        <span class="biobank-name">{{ biobank.name }}</span>
        <sup v-if="hasBiobankQuality" class="d-inline-block" aria-hidden="true">
          <InfoPopover
            faIcon="fa-regular fa-circle-check"
            textColor="text-success"
            class="ml-1 certificate-icon"
            popover-placement="bottom"
          >
            <div class="popover-content" v-for="info of qualityInfo">
              <template v-if="info">
                <div class="quality-standard-label">
                  {{ info.label }}
                </div>
                <div class="quality-standard-definition">
                  {{ info.definition }}
                </div>
              </template>
            </div>
          </InfoPopover>
        </sup>
      </router-link>
    </h5>
    <MatchesOn :viewmodel="biobank" />
  </header>
</template>

<script setup lang="ts">
import { defineProps } from "vue";
import MatchesOn from "../biobankcards-components/MatchesOn.vue";
//@ts-ignore
import { InfoPopover } from "molgenis-components";

defineProps<{
  biobank: {
    id: string;
    name: string;
  };
  hasBiobankQuality: boolean;
  qualityInfo: { label: string; definition: string }[];
}>();
</script>
