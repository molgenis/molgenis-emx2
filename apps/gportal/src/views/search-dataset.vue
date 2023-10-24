<template>
  <Page>
    <PageHeader
      title="GDI Local Portal"
      subtitle="Search for datasets"
      imageSrc="gdi-portal.jpg"
      titlePositionX="center"
      height="medium"
    />
    <PageSection
      width="large"
      class="bg-gray-050"
      :horizontalPadding="2"
      aria-labelledby="datasets-title"
    >
      <h2 id="datasets-title" class="visually-hidden">Available Datasets</h2>
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
      <MessageBox v-else-if="noDatasetFound" type="error">
        <p>
          Unable to generate dataset view. It is likely that the datasets aren't
          defined or there is information missing.
        </p>
      </MessageBox>
      <div class="sidebar-layout" v-else>
        <aside class="sidebar-menu">
          <h3>Dataset selection</h3>
          <p>
            Select one or more datasets and add them to the cart. When you are
            ready, click the "Request Access" button to apply for acess in the
            REMS.
          </p>
          <a :href="remsApplyUrl" type="button" class="btn btn-primary">
            Request Access {{ selection.length ? `(${selection.length})` : "" }}
            <ExternalLink />
          </a>
          <ButtonOutline @click="selectAll"> Select all </ButtonOutline>
          <ButtonAlt @click="clearAll"> Clear all </ButtonAlt>
        </aside>
        <div class="sidebar-main">
          <div class="dataset-card" v-for="dataset in datasets">
            <div class="dataset-info">
              <h3 class="dataset-title">{{ dataset.title }}</h3>
              <p class="dataset-description" v-if="dataset.description">
                {{ dataset.description }}
              </p>
              <ul class="dataset-tags">
                <li class="tag-type" v-if="dataset.type">
                  {{ dataset.type }}
                </li>
                <li class="tag-file-count" v-if="dataset.fileCount">
                  {{
                    dataset.fileCount > 1
                      ? `${dataset.fileCount} files`
                      : `${dataset.fileCount} file`
                  }}
                </li>
                <li
                  class="tag-format"
                  v-if="dataset.fileFormats.length"
                  v-for="format in dataset.fileFormats"
                >
                  {{ format }}
                </li>
              </ul>
            </div>
            <div class="dataset-selector">
              <input
                :id="dataset.id"
                type="checkbox"
                class="input visually-hidden"
                name="rems-selections"
                v-model="selection"
                :value="dataset.id"
                :ref="setRefs"
                @change="applySelectionStyle"
              />
              <label :for="dataset.id" class="btn btn-outline-primary">
                <span>{{
                  selection.includes(dataset.id) ? "Remove" : "Add"
                }}</span>
                <ShoppingCartIcon class="heroicon" />
              </label>
            </div>
          </div>
        </div>
      </div>
    </PageSection>
  </Page>
</template>

<script setup>
import gql from "graphql-tag";
import { request } from "graphql-request";
import { ref, watch, onBeforeMount } from "vue";
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
import { ButtonAlt, ButtonOutline } from "molgenis-components";
import { ShoppingCartIcon } from "@heroicons/vue/24/outline";

let datasets = ref([]);
let selection = ref([]);
let checkboxes = ref([]);
let remsHost = ref(null);
let remsApplyUrl = ref(null);
let noRemsHostFound = ref(false);
let noDatasetFound = ref(false);

async function getDatasets() {
  const query = gql`
    {
      Dataset {
        id
        title
        description
        distribution {
          name
          description
          type {
            name
          }
          files {
            identifier
            format {
              name
            }
            name
          }
        }
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const data = response.Dataset.map((dataset) => {
    const type = dataset.distribution[0].type.name;
    const counts = dataset.distribution[0].files
      ? dataset.distribution[0].files.length
      : null;
    const formats = dataset.distribution[0].files
      ? dataset.distribution[0].files.map((file) => file.format.name)
      : null;

    return {
      ...dataset,
      type: type,
      fileCount: counts,
      fileFormats: [...new Set(formats)],
    };
  });
  datasets.value = data;
}

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

function applySelectionStyle(event) {
  const value = event.target.value;
  const parent = event.target.parentNode.parentNode;
  if (selection.value.includes(value)) {
    parent.classList.add("card-selected");
  } else {
    parent.classList.remove("card-selected");
  }
}

onBeforeMount(() => {
  setRemsHost()
    .then(() => {
      noRemsHostFound.value = false;
    })
    .catch((err) => (noRemsHostFound.value = true));

  getDatasets().catch((err) => (noDatasetFound.value = true));
});

watch([selection], setUrl);
</script>

<style lang="scss">
.dataset-card {
  display: grid;
  grid-template-columns: 1.5fr 0.5fr;
  background-color: $gray-000;
  box-sizing: content-box;
  padding: 2em;
  border-bottom: 1px solid $gdi-brand-purple-light;

  @media screen and (min-width: 724px) {
    grid-template-columns: "info button";
  }

  .dataset-info {
    h3 {
      @include textTransform(bold);
    }

    .dataset-tags {
      list-style: none;
      display: flex;
      flex-direction: row;
      flex-wrap: wrap;
      margin: 0;
      padding: 0;
      gap: 1em;
      li {
        padding: 0.15em 0.8em;
        border-radius: 24px;
        color: $blue-800;
        background-color: $blue-050;
        font-size: 11pt;
      }
    }
  }

  .dataset-selector {
    @include flexCenterAll;

    .heroicon {
      width: 18px;
      height: 18px;
      margin-top: -0.2em;
      margin-left: 0.4em;
    }
  }

  &.card-selected {
    background-color: $gdi-brand-yellow;
  }
}
</style>
