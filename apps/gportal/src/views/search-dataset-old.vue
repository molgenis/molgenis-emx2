<template>
  <Page>
    <PageSection width="large" :horizontalPadding="0">
      <MessageBox v-if="noRemsHostFound" type="error">
        <p>
          Unable to retrieve local REMS URL. Make sure your REMS instance is
          configured with MOLGENIS. To check the status:
        </p>
        <ol>
          <li>navigate to your schema and then settings page</li>
          <li>Click the "Advanced Settings" tab</li>
          <li>
            In the settings table, confirm that there is a setting for
            <code>REMS_URL</code>
          </li>
        </ol>
        <p>
          If the setting <code>REMS_URL</code> is missing, you can add it
          manually by clicking on the plus icon. The "key" should be
          <code>REMS_URL</code> and the "setting value" should be your REMS
          host.
        </p>
      </MessageBox>
      <div v-else>
        <RoutedTableExplorer
          tableName="Dataset"
          :showColumns="[
            'id',
            'title',
            'description',
            'publisher',
            'distribution',
          ]"
          :canEdit="false"
          :canManage="false"
        >
          <template v-slot:rowcolheader>
            <h6 class="mb-0 align-text-bottom text-nowrap">Select</h6>
          </template>
          <template v-slot:rowheader="slotProps">
            <div class="checkbox">
              <input
                :id="slotProps.row.id"
                type="checkbox"
                class="input"
                name="rems-selections"
                v-model="selection"
                :value="slotProps.row.id"
                :ref="setRefs"
                @change="updateRowStyling"
              />
              <label :for="slotProps.row.id" class="label visually-hidden">
                <span>Select dataset</span>
              </label>
            </div>
          </template>
        </RoutedTableExplorer>
        <div class="d-flex flex-row justify-content-end">
          <ButtonAlt @click="clearAll"> Clear all </ButtonAlt>
          <ButtonOutline @click="selectAll"> Select all </ButtonOutline>
          <a :href="remsApplyUrl" type="button" class="btn btn-primary mx-2">
            Request Access {{ selection.length ? `(${selection.length})` : "" }}
            <ExternalLink />
          </a>
        </div>
      </div>
      {{ remsApplyUrl }}
    </PageSection>
  </Page>
</template>

<script setup>
import gql from "graphql-tag";
import { request } from "graphql-request";
import { ref, watch, onBeforeMount } from "vue";
import {
  RoutedTableExplorer,
  ButtonAlt,
  ButtonOutline,
} from "molgenis-components";
import { Page, PageSection, MessageBox } from "molgenis-viz";

import ExternalLink from "../components/icons/external-link.vue";

let selection = ref([]);
let checkboxes = ref([]);
let remsHost = ref(null);
let remsApplyUrl = ref(null);
let noRemsHostFound = ref(false);

function cleanUrl(url) {
  return url[url.length - 1] === "/" ? url.slice(0, url.length - 1) : url;
}

async function setRemsHost() {
  const query = gql`
    {
      _settings {
        key
        value
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const settings = response._settings;
  const keys = settings.map((row) => row.key);

  if (!keys.includes("REMS_URL")) {
    throw new Error("Setting for REM_URL not found");
  }

  const url = settings.filter((row) => row.key === "REMS_URL")[0].value;
  remsHost.value = cleanUrl(url);
}

function setUrl() {
  const resources = selection.value.map((item) => `resource=${item}`);
  remsApplyUrl.value = `${remsHost.value}/apply-for?${resources.join("&")}`;
}

function clearAll() {
  selection.value = [];
  remsApplyUrl.value = remsHost.value;
}

function setRefs(value) {
  if (value !== null) {
    checkboxes.value.push(value._value);
  }
}

function selectAll() {
  checkboxes.value.forEach((value) => {
    if (selection.value.indexOf(value) === -1) {
      selection.value.push(value);
    }
  });
  setUrl();
}

onBeforeMount(() => {
  setRemsHost()
    .then(() => {
      noRemsHostFound.value = false;
      console.log(remsHost.value);
    })
    .catch((err) => (noRemsHostFound.value = true));
});

watch([selection], setUrl);
</script>

<style lang="scss">
.heroicons.external-link {
  $size: 16px;
  width: $size;
  height: $size;
  margin-top: -4px;
  stroke-width: 2;
}
</style>
