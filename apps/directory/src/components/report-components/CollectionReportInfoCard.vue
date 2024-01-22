<template>
  <div class="col-md-4">
    <div class="card">
      <div class="card-body">
        <div class="card-text">
          <template v-if="info.contact">
            <h5>Contact Information</h5>
            <ul class="right-content-list">
              <template v-if="info.head">
                <li>
                  <span class="font-weight-bold mr-1">Head/PI:</span>
                  <div>{{ info.head }}</div>
                </li>
              </template>
              <li v-if="info.contact.name">
                <span class="font-weight-bold mr-1">Contact:</span>
                <div>{{ info.contact.name }}</div>
              </li>
              <li v-if="info.contact.email">
                <span
                  class="fa fa-fw fa-paper-plane mr-2"
                  aria-hidden="true"
                ></span>
                <a :href="'mailto:' + info.contact.email">
                  <span>{{ uiText["email"] }}</span>
                </a>
                <div v-if="info.contact.phone">
                  <span
                    class="fa fa-fw fa-phone mr-1"
                    aria-hidden="true"
                  ></span>
                  <a :href="'tel:' + info.contact.phone">
                    <span> {{ info.contact.phone }}</span></a
                  >
                </div>
              </li>
            </ul>
          </template>
          <template v-if="info.biobank">
            <h5>Biobank</h5>
            <ul class="right-content-list">
              <li>
                <div>
                  {{ info.biobank.name }}
                </div>
                <div>
                  {{ info.biobank.juridical_person }}
                </div>
                <div>
                  {{ info.biobank.country }}
                </div>
              </li>
              <li>
                <div v-if="info.biobank.report">
                  <span
                    class="fa fa-fw fa-address-card mr-2"
                    aria-hidden="true"
                  ></span>
                  <router-link :to="info.biobank.report">
                    <span>{{ uiText["view"] }} {{ info.biobank.name }}</span>
                  </router-link>
                </div>
                <div v-if="info.biobank.website">
                  <span
                    class="fa fa-fw fa-globe mr-2"
                    aria-hidden="true"
                  ></span>
                  <a
                    :href="info.biobank.url"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <span>Website</span>
                  </a>
                </div>
                <div v-if="info.biobank.email">
                  <span
                    class="fa fa-fw fa-paper-plane mr-2"
                    aria-hidden="true"
                  />
                  <a :href="'mailto:' + info.biobank.email">
                    <span>{{ uiText["email"] }}</span>
                  </a>
                </div>
              </li>
              <li>
                <span class="font-weight-bold mr-2">Biobank id:</span>
                <span class="biobank-id">{{ info.biobank.id }}</span>
              </li>
            </ul>
          </template>
          <template v-if="info.also_known">
            <h5>Also Known In</h5>
            <ReportDetailsList :reportDetails="info.also_known" />
          </template>
          <template v-if="info.networks && info.networks.length > 0">
            <h5>Networks</h5>
            <ul class="right-content-list">
              <li>
                <div
                  class="info-list"
                  v-for="(network, index) in info.networks"
                  :key="`${network.name}-${index}`"
                >
                  <span class="font-weight-bold mr-2">Name:</span>
                  <span>{{ network.name }}</span>
                  <div>
                    <span
                      class="fa fa-fw fa-address-card mr-2"
                      aria-hidden="true"
                    />
                    <router-link :to="network.report">
                      <span>
                        {{ uiText["view"] }} {{ network.name }} network
                      </span>
                    </router-link>
                  </div>
                </div>
              </li>
            </ul>
          </template>
          <template v-if="info.collaboration.length > 0">
            <h5>Collaboration</h5>
            <div class="container p-0">
              <div
                class="row"
                v-for="(collab, index) in info.collaboration"
                :key="`${collab.name}-${index}`"
              >
                <div class="col pr-0">
                  <span class="font-weight-bold">{{ collab.name }}:</span>
                </div>
                <div class="col p-0">
                  <span class="badge badge-info">{{ collab.value }}</span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, toRefs } from "vue";
import { useSettingsStore } from "../../stores/settingsStore";
import ReportDetailsList from "../../components/report-components/ReportDetailsList.vue";

const settingsStore = useSettingsStore();
const uiText = computed(() => settingsStore.uiText);
const props = defineProps(["info"]);
let { info } = toRefs(props);
</script>

<style scoped>
.biobank-id {
  word-break: break-all;
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

.info-list {
  margin-bottom: 1rem;
}

.cert-badge:not(:last-child) {
  margin-right: 1rem;
}
</style>
