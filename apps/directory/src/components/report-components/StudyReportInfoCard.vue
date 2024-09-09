<template>
  <div class="col-md-4">
    <div class="card">
      <div class="card-body">
        <div class="card-text">
          <template v-if="info.also_known">
            <h5>Also Known In</h5>
            <ReportDetailsList :reportDetails="info.also_known" />
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { toRefs } from "vue";
import ReportDetailsList from "../../components/report-components/ReportDetailsList.vue";

const props = defineProps(["info"]);
const { info } = toRefs(props);
</script>

<style scoped>
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
