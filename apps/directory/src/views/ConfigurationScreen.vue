<template>
  <div class="container-fluid p-0">
    <nav class="navbar bg-dark justify-content-start mb-4">
      <button
        type="button"
        @click="switchView('ui')"
        class="btn btn-link text-white"
        :class="{ 'editor-active': editorType === 'ui' }"
      >
        Filters
      </button>
      <button
        type="button"
        @click="switchView('editor')"
        class="btn btn-link text-white"
        :class="{ 'editor-active': editorType === 'editor' }"
      >
        JSON
      </button>
      <button
        type="button"
        @click="switchView('landingpage')"
        class="btn btn-link text-white"
        :class="{ 'editor-active': editorType === 'landingpage' }"
      >
        Landingpage
      </button>

      <div class="top-notification ml-auto">
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
            <span
              >We could not save the configuration, make sure you are logged in
              with sufficient rights.</span
            >
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

    <div v-if="editorType === 'ui'" class="row px-5 pb-3">
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
        <h3>{{ getFacetTitle(this.filterIndex) }} filter configuration</h3>
        <filter-editor
          :key="filterIndex"
          class="filter-editor"
          :config="currentConfig"
          :filterIndex="this.filterIndex"
          :title="getFacetTitle(this.filterIndex)"
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

    <!-- Advanced Editor -->
    <json-editor
      :config="currentConfig"
      @save="saveFromEditor"
      @cancel="switchView('ui')"
      v-if="editorType === 'editor'"
    />

    <!-- Diff editor -->
    <diff-editor
      v-if="editorType === 'diff'"
      :currentConfig="currentConfig"
      :newConfig="uploadedAppConfig"
      @save="saveFromEditor"
      @cancel="switchView('editor')"
    />

    <!-- Landingpage editor -->
    <div v-if="editorType === 'landingpage'" class="row px-5 pb-5">
      <landingpage-editor
        :currentConfig="currentConfig"
        @save="saveLandingpage"
        @cancel="switchView('ui')"
      />
    </div>
    <!-- standard button bar -->
    <div v-if="editorType === 'ui'" class="row px-5 pb-5">
      <div class="col pl-0">
        <button class="btn btn-primary mr-3 save-button" @click="save">
          Save configuration
        </button>
        <button v-if="dirty" class="btn btn-dark mr-3" @click="cancel">
          Cancel
        </button>

        <button class="btn btn-outline-dark mr-3" @click="download">
          Download config
        </button>
        <button class="btn btn-outline-dark" @click="upload">
          Upload config
        </button>

        <input
          type="file"
          id="file-selector"
          accept=".json"
          @change="processUpload"
        />
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
              <span
                >We could not save the configuration, make sure you are logged
                in with sufficient rights.</span
              >
            </div>
          </div>

          <div v-if="dirty" class="alert alert-warning m-0" role="alert">
            <span>You have unsaved changes</span>
          </div>
        </div>
        <small v-if="filterIndex !== -1" class="mt-4 float-right"
          >To format your file press ctrl + f</small
        >
      </div>
    </div>
  </div>
</template>

<script>
import DiffEditor from "../components/configuration/DiffEditor.vue";
import FilterEditor from "../components/configuration/FilterEditor.vue";
import FilterConfigUI from "../components/configuration/FilterConfigUI.vue";
import JsonEditor from "../components/configuration/JsonEditor.vue";
import LandingpageEditor from "../components/configuration/LandingpageEditor.vue";
import { filterTemplate } from "../filter-config/facetConfigurator";
import { useSettingsStore } from "../stores/settingsStore";
import { toRaw } from "vue";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    return { settingsStore };
  },
  components: {
    FilterConfigUI,
    DiffEditor,
    FilterEditor,
    LandingpageEditor,
    JsonEditor,
  },
  data() {
    return {
      editor: {},
      statusClosed: true,
      dirty: false,
      undoFilterSort: 0,
      editorType: "ui", // ui / editor / diff
      newAppConfig: "",
      uploadedAppConfig: "",
      jsonError: "",
      filterIndex: -1,
    };
  },
  methods: {
    getFacetTitle(index) {
      return JSON.parse(this.currentConfig).filterFacets[index].facetTitle;
    },
    switchView(view) {
      this.editorType = view;
    },

    applyChanges(changesToSave) {
      this.newAppConfig = changesToSave;
      this.saveToDatabase(changesToSave);
      this.filterIndex = -1;
    },
    deleteFilter() {
      const newConfig = this.copyConfig();
      newConfig.filterFacets.splice(this.filterIndex, 1);
      this.filterIndex = -1;
      this.saveToDatabase(newConfig);
    },
    save() {
      this.statusClosed = false;
      this.saveToDatabase(this.newAppConfig);
    },
    updateFilters(newConfig) {
      this.dirty = true;
      this.newAppConfig = newConfig;
    },
    saveFromEditor(changesToSave) {
      this.dirty = true;

      this.newAppConfig = changesToSave;
      this.saveToDatabase(changesToSave);
      this.switchView("ui");
    },
    saveLandingpage(changesToSave) {
      this.newAppConfig = changesToSave;
      this.saveToDatabase(changesToSave);
    },
    checkJSONStructure(jsonString) {
      if (typeof jsonString === "object") return;
      try {
        JSON.parse(jsonString);
        this.jsonError = "";
      } catch (e) {
        this.jsonError = e;
      }
    },
    saveToDatabase(newConfiguration) {
      this.checkJSONStructure(toRaw(newConfiguration));
      if (!this.jsonError) {
        this.settingsStore.SaveApplicationConfiguration(newConfiguration);
        this.dirty = false;
      }
    },
    cancel() {
      this.dirty = false;
      this.newAppConfig = "";
      this.editor.getModel().setValue(this.appConfig);
      this.filterIndex = -1;
    },
    download() {
      const file = new Blob([this.newAppConfig || this.appConfig], {
        type: "json",
      });
      const a = document.createElement("a");
      const url = URL.createObjectURL(file);
      a.href = url;
      a.download = `${window.location.host}-config.json`;
      document.body.appendChild(a);
      a.click();
      setTimeout(function () {
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      }, 0);
    },
    upload() {
      const fileInput = document.getElementById("file-selector");
      fileInput.click();
    },
    async processUpload(event) {
      const reader = new FileReader();
      reader.addEventListener("load", (event) => {
        this.uploadedAppConfig = atob(event.target.result.split(",")[1]);

        this.switchView("diff");
      });
      reader.readAsDataURL(event.target.files[0]);
    },
    setFilterEditIndex(newIndex) {
      this.filterIndex = newIndex;
    },
    addFilter() {
      this.dirty = true;
      const filterCount = this.currentFilterConfig.filterFacets.length;

      const newConfig =
        this.currentFilterConfig.filterFacets.push(filterTemplate);
      this.newAppConfig = JSON.stringify(newConfig);
      this.filterIndex = filterCount;
    },
  },
  computed: {
    configUpdateStatus() {
      return this.settingsStore.configUpdateStatus;
    },
    showNotification() {
      return this.configUpdateStatus > 0 && !this.statusClosed;
    },
    currentConfig() {
      return this.newAppConfig || this.appConfig;
    },
    newConfig() {
      return this.uploadedAppConfig;
    },
    appConfig() {
      const config = this.settingsStore.config || {};
      return JSON.stringify(config);
    },
    filterEditMode() {
      return this.filterIndex >= 0;
    },
  },
  watch: {
    configUpdateStatus(newStatus) {
      this.statusClosed = false;
      if (newStatus !== 0) {
        const timer = setTimeout(() => {
          this.statusClosed = true;
          clearTimeout(timer);
        }, 5000);
      }
    },
  },
  destroyed() {
    this.filterIndex = -1;
  },
  async mounted() {
    await this.settingsStore.GetApplicationConfiguration();
  },
};
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

#file-selector {
  display: none;
}
:deep(.original-in-monaco-diff-editor .view-lines),
:deep(.original-in-monaco-diff-editor .margin-view-overlays) {
  background-color: #eaeaea;
}

.filter-editor {
  margin-top: 3.1rem;
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
