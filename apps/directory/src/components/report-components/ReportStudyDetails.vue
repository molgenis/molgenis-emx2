<template>
  <div>
    <div class="d-flex">
      <report-description
        :description="study.description"
        :maxLength="500"
      ></report-description>
    </div>

    <!-- study information -->
    <view-generator :viewmodel="studyModel.viewmodel" />
  </div>
</template>

<script>
import { getStudyDetails } from "../../functions/viewmodelMapper";
import ReportDescription from "../report-components/ReportDescription.vue";
import ViewGenerator from "../generators/ViewGenerator.vue";

export default {
  name: "ReportStudyDetails",
  props: {
    study: {
      type: Object,
      required: true,
    },
  },
  components: {
    ReportDescription,
    ViewGenerator,
  },
  computed: {
    studyModel() {
      return this.study ? getStudyDetails(this.study) : {};
    },
  },
};
</script>
