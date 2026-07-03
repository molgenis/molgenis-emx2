<script setup lang="ts">
import { ref } from "vue";
import {
  Accordion,
  Page,
  PageHeader,
  PageSection,
  FileList,
  // @ts-ignore
} from "molgenis-viz";

import PrivateFiles from "../components/PrivateFiles.vue";
import Publications from "../components/Publications.vue";

const selectedLanguage = ref("");
const languageOptions = [
  "Arabic",
  "Bulgarian",
  "Croatian",
  "Czech",
  "Danish",
  "Dutch",
  "English",
  "Estonian",
  "Finnish",
  "French",
  "German",
  "Greek",
  "Hebrew",
  "Hungarian",
  "Italian",
  "Latvian",
  "Lithuanian",
  "Norwegian",
  "Polish",
  "Portuguese",
  "Romanian",
  "Slovak",
  "Slovene",
  "Spanish",
  "Swedish",
  "Turkish",
];
</script>

<template>
  <Page id="page-documents">
    <PageHeader
      imageSrc="img/erras-header.jpg"
      titlePositionX="center"
      titlePositionY="center"
    >
      <div class="erras-header p-3">
        <h1>ERN-Skin Registry</h1>
        <h2>Additional Information</h2>
      </div>
    </PageHeader>
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="2"
    >
      <h2 id="section-documents-title"></h2>
      <nav class="navbar">
        <router-link class="nav-item" :to="{ name: 'studies' }">
          Studies
        </router-link>
      </nav>
      <Accordion
        id="accReq-nav"
        title="Access Requests"
        :isOpenByDefault="false"
      >
        <FileList
          table="Files"
          filter='filter: { tags: { not_equals: "private" } }'
          labelsColumn="name"
          fileColumn="file"
        />
      </Accordion>
      <Accordion
        id="patient-nav"
        title="Are you a patient?"
        :isOpenByDefault="false"
        :headingStyle="3"
      >
        <ul>
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
      </Accordion>
      <Accordion
        id="general-nav"
        title="General Documents"
        :isOpenByDefault="false"
      >
        <div v-if="$route.params.email && $route.params.email !== 'anonymous'">
          <Accordion
            id="mySubfolder-nav"
            title="Matrix Informed Consents"
            :isOpenByDefault="false"
          >
            <div
              style="display: flex; flex-direction: column; max-width: 250px"
            >
              <label for="language-select" style="margin-bottom: 6px">
                Select ICF language:
              </label>
              <select
                class="custom-select"
                id="language-select"
                v-model="selectedLanguage"
                size="7"
                style="padding: 6px; font-size: 16px"
              >
                <option
                  v-for="option in languageOptions"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </option>
              </select>
            </div>
            <div v-if="selectedLanguage" class="mt-3">
              <PrivateFiles
                :user="($route.params?.email as string)"
                :labelValue="selectedLanguage"
              />
            </div>
          </Accordion>
          <Accordion
            id="mySubfolder-nav"
            title="Templates Uploads"
            :isOpenByDefault="false"
          >
            <PrivateFiles
              :user="($route.params?.email as string)"
              labelValue="template"
            />
          </Accordion>
          <Accordion
            id="mySubfolder-nav"
            title="Other general documents"
            :isOpenByDefault="false"
          >
            <strong>Publications</strong>
            <Publications
              table="Publications"
              labelsColumn="title"
              doiColumn="doi"
            />
          </Accordion>
        </div>
        <div v-else>
          <strong>Publications</strong>
          <Publications
            table="Publications"
            labelsColumn="title"
            doiColumn="doi"
          />
        </div>
      </Accordion>
    </PageSection>
  </Page>
</template>
