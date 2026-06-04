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
      <nav class="navbar">
        <router-link class="nav-item" :to="{ name: 'studies' }"
          >Studies</router-link
        >
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
        <p v-if="user !== 'anonymous' && user">
          <Accordion
            id="mySubfolder-nav"
            title="Informed consents"
            :isOpenByDefault="false"
          >
            <div
              style="display: flex; flex-direction: column; max-width: 250px"
            >
              <label for="language-select" style="margin-bottom: 6px">
                Select ICF language:
              </label>
              <select
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
            <div v-if="selectedLanguage">
              <PrivateFiles :labelValue="selectedLanguage" />
            </div>
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
        </p>
        <p v-else>
          <strong>Publications</strong>
          <Publications
            table="Publications"
            labelsColumn="title"
            doiColumn="doi"
          />
        </p>
      </Accordion>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import {
  Accordion,
  Page,
  PageSection,
  FileList,
  MessageBox,
} from "molgenis-viz";
import CustomPageHeader from "../components/CustomPageHeader.vue";
import PrivateFiles from "../components/PrivateFiles.vue";
import Publications from "../components/Publications.vue";
import { ref } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";

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
  const response = await request(
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

<style lang="scss">
.navbar {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: #0084b4;
}

.nav-item {
  text-decoration: none;
  color: white;
}

.nav-item:hover {
  font-weight: bold;
  color: white;
}

#accordion-accReq-nav {
  .accordion-heading {
    background-color: #00a453;
    border-color: #00a453;
    color: #ffffff;
  }
}

#accordion-patient-nav {
  .accordion-heading {
    background-color: #f48b31;
    border-color: #f48b31;
    color: #ffffff;
  }
}

#accordion-general-nav {
  .accordion-heading {
    background-color: #eb212e;
    border-color: #eb212e;
    color: #ffffff;
  }
}
</style>
