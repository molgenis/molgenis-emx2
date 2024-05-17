<template>
  <table class="mg-report-details-list mb-3">
    <caption v-show="false">
      Details list
    </caption>
    <template v-for="(detail, detailKey) in reportDetails">
      <!-- Header -->
      <tr v-if="showRow(detail)" :key="detailKey">
        <th scope="row" class="pr-1" v-if="showKey(detail.type)">
          {{ detailKey }}:
        </th>

        <!--Type bool-->
        <td v-if="detail.type === 'bool'">
          <span v-if="detail.value" class="badge badge-info">yes</span>
          <span v-else class="badge badge-info">no</span>
        </td>
        <!--Type string-->
        <td v-else-if="detail.type.includes('string')" colspan="2">
          {{ detail.value }}
        </td>
        <!--Type url-->
        <td v-else-if="detail.type === 'url'">
          <a :href="detail.value" target="_blank" rel="noopener noreferrer">
            <i class="fa fa-fw fa-globe" aria-hidden="true" />
            <span class="mg-icon-text">{{ detail.label || "Website" }}</span>
          </a>
        </td>
        <!--Type email-->
        <td v-else-if="detail.type === 'email'" colspan="2">
          <a :href="'mailto:' + detail.value">
            <i class="fa fa-fw fa-paper-plane" aria-hidden="true" />
            <span class="mg-icon-text">{{ uiText["email"] }}</span>
          </a>
        </td>
        <!--Type phone-->
        <td v-else-if="detail.type === 'phone'">
          <i class="fa fa-fw fa-phone" aria-hidden="true" />
          <span class="mg-icon-text">{{ detail.value }}</span>
        </td>
        <!--Type list-->
        <td v-else-if="detail.type === 'list' && detail.value?.length > 0">
          <span
            v-for="(val, index) in detail.value"
            class="m-1 badge"
            :key="index"
            :class="'badge-' + (detail.badgeColor || 'success')"
          >
            {{ val }}
          </span>
        </td>
        <!--Type report-->
        <td v-else-if="detail.type === 'report'" colspan="2">
          <router-link :to="detail.value">
            <i class="fa fa-fw fa-address-card" aria-hidden="true" />
            <span class="mg-icon-text">Overview</span>
          </router-link>
        </td>
      </tr>
    </template>
  </table>
</template>

<style scoped>
.mg-icon-text {
  margin-left: 0.2rem;
}
</style>

<script setup lang="ts">
import { computed } from "vue";
import { useSettingsStore } from "../../stores/settingsStore";
import { IReportDetail } from "./reportInterfaces";
const settingsStore = useSettingsStore();

const { reportDetails } = defineProps<{
  reportDetails: Record<string, IReportDetail>;
}>();
const uiText = computed(() => settingsStore.uiText);

function showRow(value: IReportDetail) {
  return value.value?.length || value.type === "bool";
}

function showKey(type: string) {
  return ["bool", "string-with-key", "list"].includes(type);
}
</script>
