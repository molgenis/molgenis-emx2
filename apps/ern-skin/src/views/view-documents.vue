<script setup lang="ts">
import gql from "graphql-tag";
import { request } from "graphql-request";

import { ref } from "vue";
import {
  Accordion,
  Page,
  PageHeader,
  PageSection,
  FileList,
  // @ts-ignore
} from "molgenis-viz";

import type { ISession } from "../../../tailwind-components/types/types";
interface ISessionResponse {
  _session: ISession;
}

import PrivateFiles from "../components/PrivateFiles.vue";
import Publications from "../components/Publications.vue";

const error = ref<Error | null>(null);
const loading = ref(true);
const user = ref<string | null>(null);

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

async function getSession() {
  const query = gql`
    query {
      _session {
        email
      }
    }
  `;
  const response: ISessionResponse = await request(
    "/api/graphql",
    query,
    {},
    { credentials: "include" }
  );
  user.value = response._session?.email || null;
}

getSession()
  .catch((err: any) => {
    if (err.response?.errors?.length) {
      error.value = err.response.errors[0].message;
    } else {
      error.value = err;
    }
  })
  .finally(() => {
    loading.value = false;
  });
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
        <div v-if="user && user !== 'anonymous'">
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
              <PrivateFiles :labelValue="selectedLanguage" />
            </div>
          </Accordion>
          <Accordion
            id="mySubfolder-nav"
            title="Templates Uploads"
            :isOpenByDefault="false"
          >
            <PrivateFiles labelValue="template" />
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
