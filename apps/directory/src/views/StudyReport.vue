<template>
  <div class="container mg-network-report-card">
    <div
      v-if="!loaded"
      class="d-flex justify-content-center align-items-center spinner-container"
    >
      <Spinner />
    </div>
    <div v-else class="container-fluid">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center bg-white">
          <Breadcrumb
            class="directory-nav"
            :crumbs="{
              [uiText['home']]: '../#/',
              [study.title]: '/',
            }"
          />
        </div>
      </div>

      <div class="row" v-if="study">
        <div class="col">
          <report-title type="Study" :name="study.title" />
          <div class="container">
            <div class="row">
              <div class="container p-0">
                <div class="row">
                  <div class="col-md-8">
                    <report-study-details v-if="study" :study="study" />
                  </div>
                  <study-report-info-card :info="info" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Breadcrumb, Spinner } from "molgenis-components";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import ReportStudyDetails from "../components/report-components/ReportStudyDetails.vue";
import StudyReportInfoCard from "../components/report-components/StudyReportInfoCard.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import { getStudyReportInformation } from "../functions/viewmodelMapper";
import { useStudyStore } from "../stores/studyStore";
import { useSettingsStore } from "../stores/settingsStore";

const settingsStore = useSettingsStore();
const studyStore = useStudyStore();

const route = useRoute();
const study = ref({});

let loaded = ref(false);

loadStudyReport(route.params.id);

const uiText = computed(() => settingsStore.uiText);

const info = computed(() => {
  return getStudyReportInformation(study.value);
});

function loadStudyReport(id) {
  loaded.value = false;
  studyStore.getStudyReport(id).then((result) => {
    study.value = result.Studies.length ? result.Studies[0] : {};
    loaded.value = true;
  });
}
</script>
