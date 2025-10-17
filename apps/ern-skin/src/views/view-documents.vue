<template>
  <Page id="page-documents">
    <CustomPageHeader
      class="erras-header"
      title="ERN-Skin Registry"
      subtitle="Additional Information"
      imageSrc="img/erras-header.jpg"
      height="xlarge"
      title-position-x="center"
      title-position-y="center"
    />
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="2"
    >
      <h2 id="section-documents-title"></h2>
      <div>
        <button
          style="
            background-color: #0084b4;
            border-color: #0084b4;
            color: #ffffff;
          "
          @click="toggleStudies"
        >
          {{ studiesOpen ? "Studies" : "Studies" }}
        </button>
        <ul v-if="studiesOpen">
          <Studies table="Studies" labelsColumn="title" />
        </ul>
      </div>
      <div>
        <button
          style="
            background-color: #00a453;
            border-color: #00a453;
            color: #ffffff;
          "
          @click="toggleAccess"
        >
          {{ accessOpen ? "Access Requests" : "Access Requests" }}
        </button>
        <ul v-if="accessOpen">
          <FileList table="Files" labelsColumn="name" fileColumn="file" />
        </ul>
      </div>
      <div>
        <button
          style="
            background-color: #f48b31;
            border-color: #f48b31;
            color: #ffffff;
          "
          @click="togglePatient"
        >
          {{ patientOpen ? "Are you a patient?" : "Are you a patient?" }}
        </button>
        <ul v-if="patientOpen">
          <li>
            Informed consent: please check with your treating doctor who will
            provide you with all explanations and documents in local language.
          </li>
          <li>
            Withdrawal of consent: please check with your treating doctor who
            will provide you with the information for the withdrawal of your
            consent.
          </li>
          <li>Contact point: skin-registry@ern-skin.eu</li>
        </ul>
      </div>
      <div>
        <button
          style="
            background-color: #eb212e;
            border-color: #eb212e;
            color: #ffffff;
          "
          @click="toggleGeneral"
        >
          {{ generalOpen ? "General Documents" : "General Documents" }}
        </button>
        <ul v-if="generalOpen">
          <big><strong>Publications</strong></big>
          <Publications
            table="Publications"
            labelsColumn="title"
            doiColumn="doi"
          />
        </ul>
      </div>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageSection, FileList } from "molgenis-viz";
import CustomPageHeader from "../components/CustomPageHeader.vue";
import Publications from "../components/Publications.vue";
import Studies from "../components/Studies.vue";
import { ref } from "vue";

// Define reactive state
const studiesOpen = ref(false);
const accessOpen = ref(false);
const patientOpen = ref(false);
const generalOpen = ref(false);

// Toggle method
function toggleStudies() {
  studiesOpen.value = !studiesOpen.value;
}

function toggleAccess() {
  accessOpen.value = !accessOpen.value;
}

function togglePatient() {
  patientOpen.value = !patientOpen.value;
}

function toggleGeneral() {
  generalOpen.value = !generalOpen.value;
}

// Expose properties/methods to the parent
defineExpose({
  studiesOpen,
  accessOpen,
  patientOpen,
  generalOpen,
  toggleStudies,
  toggleAccess,
  togglePatient,
  toggleGeneral,
});
</script>
