<template>
  <div class="container-fluid p-0">
    <nav class="navbar bg-dark justify-content-start mb-4">
      <button
        type="button"
        @click="switchView(views.ui)"
        class="btn btn-link text-white"
        :class="{ 'editor-active': editorType === views.ui }"
      >
        Filters
      </button>
      <button
        type="button"
        @click="switchView(views.editor)"
        class="btn btn-link text-white"
        :class="{ 'editor-active': editorType === views.editor }"
      >
        JSON
      </button>
      <button
        type="button"
        @click="switchView(views.landingpage)"
        class="btn btn-link text-white"
        :class="{ 'editor-active': editorType === views.landingpage }"
      >
        Landingpage
      </button>

      <div class="top-notification ml-auto">
        <div v-show="showNotification">
          <div
            v-if="configUpdateStatus === 204"
            class="alert alert-success m-0"
            role="alert"
            @click="statusClosed = true"
          >
            Configuration saved!
          </div>
          <div
            v-else
            class="alert alert-warning m-0"
            role="alert"
            @click="statusClosed = true"
          >
            We could not save the configuration, make sure you are logged in
            with sufficient rights.
          </div>
        </div>

        <div v-if="dirty" class="alert alert-warning m-0" role="alert">
          <span>You have unsaved changes</span>
        </div>
      </div>
    </nav>

    <div class="row">
      <div v-if="jsonError" class="alert alert-danger ml-5" role="alert">
        <span>{{ jsonError }}</span>
      </div>
    </div>

    <a href="" ref="download" class="hidden"></a>

    <div
      v-if="editorType === views.ui"
      class="row px-5 pb-3"
      :key="filterIndex"
    >
      <div class="col-6">
        <FilterConfigUI
          :config="currentConfig"
          :hasUpdated="configUpdateStatus"
          @update="updateFilters"
          @add="addFilter"
          @edit="setFilterEditIndex"
        />
      </div>
      <div class="col-6" v-if="filterEditMode">
        <h3>{{ getFacetTitle(filterIndex) }} filter configuration</h3>
        <div class="editor-alignment">
          <small v-if="filterIndex !== -1">
            To format your file press <kbd>ctrl</kbd> + <kbd>alt</kbd> +
            <kbd>f</kbd>
          </small>
        </div>
        <FilterEditor
          :key="filterIndex"
          class="filter-editor"
          :config="currentConfig"
          :filterIndex="filterIndex"
          :title="getFacetTitle(filterIndex)"
          @filterUpdate="applyChanges"
          @delete="deleteFilter"
        />

        <small>
          <pre class="code-help">
{
  facetTitle: "My facet"                                     /** a custom 'human readable' text for on the button                                              */
  component: "CheckboxFilter"                                /** a custom specified component, or just the default                                             */
  sourceTable: "myTable"                                     /** the table where the options are coming from.                                                  */
  applyToColumn: "myColumn"                                  /** the column in the main table to apply the filter on.                                          */
  filterValueAttribute:"id"                                  /** specify a column name if you want a different column for the value                            */
  filterLabelAttribute: "name"                               /** specify if you want to use another column as the label for the filter option, instead of name */
  ontologyIdentifiers: []                                    /** for use when you have multiple ontologies in a single table, e.g. orhpa and icd               */
  trueOption: { text: "When clicked is true", value: true }, /** use this for a togglefilter                                                                   */
  matchTypeForFilter: "any"                                  /** if it has been selected from bookmark, it will be applied here.                               */
  showMatchTypeSelector: true                                /** if you want to make match all / match any available                                           */
  negotiatorRequestString: "From my filterfacet: "           /** the part that will be send to the negotiator as to indicate what it is                        */
  showFacet: true                                            /** if this filter should be shown at start                                                       */
  adaptive: false                                            /** if the filters options should react on search results                                         */
  sortColumn: "name"                                         /** specify a column to apply sorting on                                                          */
  sortDirection: "asc"                                       /** the direction to sort                                                                         */
}
          </pre>
        </small>
      </div>
    </div>

    <JsonEditor
      v-show="editorType === views.editor"
      :config="currentConfig"
      @dirty="(isDirty) => (dirty = isDirty)"
      @save="saveFromEditor"
      @cancel="cancel"
      @diff="showDiffEditor"
    />

    <DiffEditor
      v-if="editorType === views.diff"
      :currentConfig="diffAppConfig"
      :newConfig="uploadedAppConfig"
      @save="(changesToSave) => saveFromEditor(changesToSave, views.diff)"
      @cancel="switchView(views.editor)"
    />

    <div v-if="editorType === views.landingpage" class="row px-5 pb-5">
      <LandingpageEditor
        :currentConfig="currentConfig"
        @save="saveLandingpage"
        @cancel="cancel"
      />
    </div>

    <!-- standard button bar -->
    <div v-if="editorType === views.ui" class="row mt-3 px-5 pb-5">
      <div class="col">
        <button class="btn btn-primary mr-3 save-button" @click="save">
          Save configuration
        </button>
        <button v-if="dirty" class="btn btn-dark mr-3" @click="cancel">
          Cancel
        </button>
      </div>
      <div>
        <div class="row">
          <div v-show="showNotification">
            <div
              v-if="configUpdateStatus === 204"
              class="alert alert-success m-0 mr-3"
              role="alert"
              @click="statusClosed = true"
            >
              <span>Configuration saved!</span>
            </div>
            <div
              v-else
              class="alert alert-warning m-0"
              role="alert"
              @click="statusClosed = true"
            >
              We could not save the configuration, make sure you are logged in
              with sufficient rights.
            </div>
          </div>

          <div v-if="dirty" class="alert alert-warning m-0" role="alert">
            You have unsaved changes
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, toRaw, watch } from "vue";
import DiffEditor from "../components/configuration/DiffEditor.vue";
import FilterConfigUI from "../components/configuration/FilterConfigUI.vue";
import FilterEditor from "../components/configuration/FilterEditor.vue";
import JsonEditor from "../components/configuration/JsonEditor.vue";
import LandingpageEditor from "../components/configuration/LandingpageEditor.vue";
import { filterTemplate } from "../filter-config/facetConfigurator";
import { useSettingsStore } from "../stores/settingsStore";

enum views {
  ui,
  editor,
  diff,
  landingpage,
}

const settingsStore = useSettingsStore();

const statusClosed = ref(true);
const dirty = ref(false);
const editorType = ref<views>();
const newAppConfig = ref("");
const jsonError = ref("");
const filterIndex = ref(-1);
const uploadedAppConfig = ref("");
const diffAppConfig = ref("");

const configUpdateStatus = computed(() => {
  return settingsStore.configUpdateStatus;
});

const showNotification = computed(() => {
  return configUpdateStatus.value > 0 && !statusClosed.value;
});

const currentConfig = computed(() => {
  return newAppConfig.value || appConfig.value;
});

const appConfig = computed(() => {
  const config = settingsStore.config || {};
  return JSON.stringify(config);
});

const filterEditMode = computed(() => {
  return filterIndex.value >= 0;
});

watch(configUpdateStatus, (newStatus) => {
  statusClosed.value = false;
  if (newStatus !== 0) {
    const timer = setTimeout(() => {
      statusClosed.value = true;
      clearTimeout(timer);
    }, 5000);
  }
});

onMounted(async () => {
  editorType.value = views.ui;
});

function getFacetTitle(index: number) {
  return JSON.parse(currentConfig.value).filterFacets[index].facetTitle;
}

function switchView(view: views) {
  editorType.value = view;
}

function showDiffEditor(diff: {
  currentAppConfig: string;
  uploadedAppConfig: string;
}) {
  diffAppConfig.value = diff.currentAppConfig;
  uploadedAppConfig.value = diff.uploadedAppConfig;
  switchView(views.diff);
}

function applyChanges(changesToSave: string) {
  newAppConfig.value = changesToSave;
  saveToDatabase(changesToSave);
  filterIndex.value = -1;
}

function save() {
  statusClosed.value = false;

  saveToDatabase(newAppConfig.value);
}

function updateFilters(newConfig: string) {
  dirty.value = true;
  newAppConfig.value = newConfig;
}

function saveFromEditor(changesToSave: string, view: views) {
  dirty.value = true;

  newAppConfig.value = changesToSave;
  saveToDatabase(changesToSave);

  if (view === views.diff) {
    switchView(views.editor);
  }
}

function saveLandingpage(changesToSave: string) {
  newAppConfig.value = changesToSave;
  saveToDatabase(changesToSave);
}

function checkJSONStructure(jsonString: string | Record<string, any>) {
  if (typeof jsonString === "object") {
    return;
  } else {
    try {
      JSON.parse(jsonString);
      jsonError.value = "";
    } catch (error: any) {
      jsonError.value = error;
    }
  }
}

function saveToDatabase(newConfiguration: string) {
  checkJSONStructure(toRaw(newConfiguration));
  if (!jsonError.value) {
    settingsStore.SaveApplicationConfiguration(newConfiguration);
    dirty.value = false;
  }
}

function cancel() {
  dirty.value = false;
  newAppConfig.value = "";
  filterIndex.value = -1;
}

function setFilterEditIndex(newIndex: number) {
  filterIndex.value = newIndex;
}

function addFilter() {
  dirty.value = true;
  const config = JSON.parse(currentConfig.value);

  config.filterFacets.unshift(filterTemplate);
  newAppConfig.value = JSON.stringify(config);
  filterIndex.value = 0;
}

function deleteFilter() {
  const newConfig = JSON.parse(currentConfig.value);
  newConfig.filterFacets.splice(filterIndex, 1);
  newAppConfig.value = JSON.stringify(newConfig);
  filterIndex.value = -1;
  saveToDatabase(newConfig);
}
</script>

<style scoped>
.top-notification {
  display: flex;
  justify-content: flex-end;
}

.top-notification > .alert {
  border-radius: 0;
  margin-right: 1px !important;
  max-height: calc(3rem - 2px);
}

.code-help {
  margin-top: 4rem;
}

.editor-active {
  text-decoration: underline;
}

.navbar {
  height: 3rem;
  max-height: 3rem;
  padding-left: 2rem;
}

:deep(.original-in-monaco-diff-editor .view-lines),
:deep(.original-in-monaco-diff-editor .margin-view-overlays) {
  background-color: #eaeaea;
}

.filter-editor {
  margin-top: 0.75rem;
  height: 40%;
  width: 100%;
  border: 1px solid black;
}

.alert:hover {
  cursor: pointer;
}

.save-button {
  width: 14rem;
}
</style>
