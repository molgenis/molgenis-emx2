import { QueryEMX2 } from "molgenis-components";
import { defineStore } from "pinia";
import { computed, ref } from "vue";
import useErrorHandler from "../composables/errorHandler";
import { initialFilterFacets } from "../filter-config/initialFilterFacets";
import { i18n } from "../i18n/i18n";
import initialBiobankColumns from "../property-config/initialBiobankColumns";
import initialBiobankReportColumns from "../property-config/initialBiobankReportColumns";
import initialCollectionColumns from "../property-config/initialCollectionColumns";
import initialLandingpage from "../property-config/initialLandingpage";
import initialStudyColumns from "../property-config/initialStudyColumns";
/**
 * Settings store is where all the configuration of the application is handled.
 * This means that user config from the database is merged with the defaults here.
 */

const { setError, clearError } = useErrorHandler();

export const useSettingsStore = defineStore("settingsStore", () => {
  const session = ref({});
  const configUpdateStatus = ref(0);
  const currentPage = ref(1);
  const configurationFetched = ref(false);
  const config = ref({
    language: "en",
    graphqlEndpoint: "graphql",
    negotiatorType: "v3",
    negotiatorUrl: "https://negotiator.acc.bbmri-eric.eu/api/v3/requests",
    negotiatorUsername: "",
    negotiatorPassword: "",
    biobankColumns: initialBiobankColumns,
    biobankReportColumns: initialBiobankReportColumns,
    collectionColumns: initialCollectionColumns,
    studyColumns: initialStudyColumns,
    filterFacets: initialFilterFacets,
    biobankCardShowCollections: true,
    landingpage: initialLandingpage,
    pageSize: 12,
    i18n,
    banner: ``,
    footer: ``,
  });

  const showSettings = computed(() => {
    return session.value.roles?.includes("Manager");
  });

  const uiText = computed(() => {
    return config.value.i18n[config.value.language];
  });

  initializeConfig();

  async function initializeConfig() {
    if (configurationFetched.value) return;

    await loadConfig();
  }

  async function loadConfig() {
    configurationFetched.value = false;
    clearError();

    let configPromise;
    try {
      configPromise = new QueryEMX2(config.value.graphqlEndpoint)
        .table("_settings")
        .select(["key", "value"])
        .execute();
    } catch (error) {
      setError(error);
      return;
    }

    const response = await configPromise;

    const savedDirectoryConfig = response._settings.find(
      (setting) => setting.key === "directory"
    );

    if (savedDirectoryConfig?.value) {
      config.value = JSON.parse(decodeURI(savedDirectoryConfig.value));
    }

    configurationFetched.value = true;
  }

  function setSessionInformation(newSession) {
    session.value = newSession;
  }

  async function SaveApplicationConfiguration(configuration) {
    configUpdateStatus.value = 0;

    const updateResult = await new QueryEMX2(
      config.value.graphqlEndpoint
    ).saveSetting("directory", configuration);

    configUpdateStatus.value = updateResult.includes("success") ? 204 : 401;
    loadConfig();
  }

  async function UpdateConfig(newConfig) {
    config.value = newConfig;
  }

  return {
    config,
    configurationFetched,
    configUpdateStatus,
    currentPage,
    showSettings,
    uiText,
    initializeConfig,
    setSessionInformation,
    SaveApplicationConfiguration,
    UpdateConfig,
  };
});
