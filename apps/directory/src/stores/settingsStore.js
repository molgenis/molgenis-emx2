import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { i18n } from "../i18n/i18n";
import { initialFilterFacets } from "../filter-config/initialFilterFacets";
import initialCollectionColumns from "../property-config/initialCollectionColumns";
import initialBiobankColumns from "../property-config/initialBiobankColumns";
import initialBiobankReportColumns from "../property-config/initialBiobankReportColumns";
import initialLandingpage from "../property-config/initialLandingpage";
import QueryEMX2 from "../functions/queryEMX2";
/**
 * Settings store is where all the configuration of the application is handled.
 * This means that user config from the database is merged with the defaults here.
 */
export const useSettingsStore = defineStore("settingsStore", () => {
  let session = ref({});
  let configUpdateStatus = ref(0);

  const currentPage = ref(1);

  let config = ref({
    language: "en",
    graphqlEndpoint: "graphql",
    negotiatorType: "eric-negotiator",
    biobankColumns: initialBiobankColumns,
    biobankReportColumns: initialBiobankReportColumns,
    collectionColumns: initialCollectionColumns,
    filterFacets: initialFilterFacets,
    filterMenuInitiallyFolded: false,
    biobankCardShowCollections: true,
    landingpage: initialLandingpage,
    pageSize: 12,
    i18n,
  });

  async function initializeConfig() {
    const settingsResult = await new QueryEMX2(config.value.graphqlEndpoint)
      .table("_settings")
      .select(["key", "value"])
      .execute();

    const savedDirectoryConfig = settingsResult._settings.find(
      (sr) => sr.key === "directory"
    );

    if (savedDirectoryConfig && savedDirectoryConfig.value) {
      config.value = JSON.parse(decodeURI(savedDirectoryConfig.value));
    }
  }

  /** for when user logs-in / out */
  function setSessionInformation(newSession) {
    session.value = newSession;
  }

  async function getSessionInformation() {
    const sessionResult = await new QueryEMX2(config.value.graphqlEndpoint)
      .table("_session")
      .select(["email", "roles"])
      .execute();
    session.value = { ...sessionResult._session };
  }

  async function getCurrentSession() {
    if (!Object.keys(session.value).length) {
      await getSessionInformation();
    }
    return session.value;
  }

  const showSettings = computed(() => {
    return session.value.roles?.includes("Manager");
  });

  const uiText = computed(() => {
    return config.value.i18n[config.value.language];
  });

  async function GetApplicationConfiguration() {
    /** Fetch the latest config, if applicable */
    await initializeConfig();
    return config.value;
  }
  async function SaveApplicationConfiguration(configuration) {
    configUpdateStatus.value = 0;

    const updateResult = await new QueryEMX2(
      config.value.graphqlEndpoint
    ).saveSetting("directory", configuration);

    configUpdateStatus.value = updateResult.includes("success") ? 204 : 401;
  }
  async function UpdateConfig(newConfig) {
    config.value = newConfig;
  }

  return {
    config,
    currentPage,
    getCurrentSession,
    initializeConfig,
    UpdateConfig,
    GetApplicationConfiguration,
    setSessionInformation,
    showSettings,
    SaveApplicationConfiguration,
    configUpdateStatus,
    uiText,
  };
});
