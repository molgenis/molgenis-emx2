<script setup lang="ts">
import { ref } from "vue";
import DetailPageLayout from "../../components/layout/DetailPageLayout.vue";
import SideNav from "../../components/SideNav.vue";
import PageHeader from "../../components/PageHeader.vue";
import BreadCrumbs from "../../components/BreadCrumbs.vue";
import ContentBlocks from "../../components/content/ContentBlocks.vue";
import ContentBlock from "../../components/content/ContentBlock.vue";
import RecordColumn from "../../components/display/RecordColumn.vue";
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";

const showSideNav = ref(true);
const clickLog = ref<string[]>([]);

const sections = [
  { id: "description", label: "Description" },
  { id: "general-design", label: "General Design" },
  { id: "population", label: "Population" },
  { id: "organisations", label: "Organisations" },
  { id: "publications", label: "Publications" },
  { id: "access-conditions", label: "Access Conditions" },
];

const crumbs = [
  { label: "Home", url: "/" },
  { label: "Collections", url: "/collections" },
  { label: "Example Study", url: "" },
];

// Mock columns for General Design section
const designColumns: IColumn[] = [
  { id: "studyType", label: "Study type", columnType: "STRING" },
  { id: "startYear", label: "Start year", columnType: "INT" },
  { id: "dataCollection", label: "Data collection", columnType: "STRING" },
  { id: "design", label: "Design", columnType: "TEXT" },
  { id: "isActive", label: "Currently active", columnType: "BOOL" },
];

// Mock columns for Population section
const populationColumns: IColumn[] = [
  { id: "countries", label: "Countries", columnType: "STRING_ARRAY" },
  {
    id: "numberOfParticipants",
    label: "Number of participants",
    columnType: "INT",
  },
  { id: "ageGroups", label: "Age groups", columnType: "STRING" },
  { id: "inclusionCriteria", label: "Inclusion criteria", columnType: "TEXT" },
  {
    id: "leadOrganisation",
    label: "Lead organisation",
    columnType: "REF",
    refTableId: "Organisations",
    refSchemaId: "catalogue",
    refLabel: "name",
    refLabelDefault: "name",
    refLinkId: "id",
  } as IRefColumn,
];

// Mock columns for Access Conditions section
const accessColumns: IColumn[] = [
  { id: "accessConditions", label: "Access conditions", columnType: "STRING" },
  { id: "dataUseConditions", label: "Data use conditions", columnType: "TEXT" },
  { id: "accessFee", label: "Access fee", columnType: "DECIMAL" },
  { id: "website", label: "Website", columnType: "HYPERLINK" },
  { id: "contactEmail", label: "Contact email", columnType: "EMAIL" },
  { id: "lastUpdated", label: "Last updated", columnType: "DATE" },
];

// Mock row data for General Design
const designRow: Record<string, any> = {
  studyType: "Cohort study",
  startYear: 1990,
  dataCollection: "Longitudinal",
  design: "Birth cohort with ongoing follow-up",
  isActive: true,
};

// Mock row data for Population
const populationRow: Record<string, any> = {
  countries: ["Netherlands", "Belgium", "Germany"],
  numberOfParticipants: 15000,
  ageGroups: "0-4, 5-9, 10-14, 15-19, 20-29, 30-39",
  inclusionCriteria: "Born in the study region during enrollment period",
  leadOrganisation: { name: "University Medical Center", id: "umc-001" },
};

// Mock row data for Access Conditions
const accessRow: Record<string, any> = {
  accessConditions: "Research use only",
  dataUseConditions: "Requires ethics approval and data access agreement",
  accessFee: 0.0,
  website: "https://example-study.org",
  contactEmail: "contact@example-study.org",
  lastUpdated: "2024-06-15",
};

// Click handler for REF fields
const getRefClickAction = (col: IColumn, val: any) => () => {
  const message = `Clicked: ${col.label} -> ${JSON.stringify(val)}`;
  clickLog.value.unshift(message);
  if (clickLog.value.length > 5) clickLog.value.pop();
  console.log("Ref clicked:", col.id, val);
};
</script>

<template>
  <div class="p-4">
    <h1 class="text-heading-xl mb-4">DetailPageLayout Story</h1>
    <p class="text-body-base mb-4">
      This layout matches the catalogue detail pages. Toggle the checkbox to
      hide/show the side navigation. The General Design, Population, and Access
      Conditions sections use RecordColumn components.
    </p>

    <fieldset class="border border-gray-400 p-4 mb-6 rounded">
      <legend class="px-2 font-semibold">Props</legend>
      <div class="flex items-center gap-2">
        <input
          id="show-side-nav"
          type="checkbox"
          v-model="showSideNav"
          class="hover:cursor-pointer"
        />
        <label for="show-side-nav" class="hover:cursor-pointer">
          showSideNav (toggle to hide/show side navigation)
        </label>
      </div>
    </fieldset>

    <div
      v-if="clickLog.length"
      class="mb-6 p-4 bg-blue-50 rounded border border-blue-200"
    >
      <h3 class="font-semibold mb-2">Click Log (REF field clicks)</h3>
      <ul class="text-sm space-y-1">
        <li v-for="(log, i) in clickLog" :key="i" class="text-gray-700">
          {{ log }}
        </li>
      </ul>
    </div>

    <DetailPageLayout :show-side-nav="showSideNav">
      <template #header>
        <PageHeader
          id="page-header"
          title="Example Study"
          description="Example Longitudinal Study of Health and Development - A comprehensive cohort study tracking health outcomes over multiple decades."
        >
          <template #prefix>
            <BreadCrumbs :crumbs="crumbs" />
          </template>
        </PageHeader>
      </template>

      <template #side>
        <SideNav title="EXAMPLE" :sections="sections" :scroll-offset="80" />
      </template>

      <template #main>
        <ContentBlocks>
          <ContentBlock id="description" title="Description">
            <p class="text-body-base mb-4">
              This is a longitudinal population-based study that has been
              following participants since birth. The study collects data on a
              wide range of health, social, and environmental factors to
              understand how early life experiences affect later health
              outcomes.
            </p>
            <p class="text-body-base">
              The study includes comprehensive phenotypic data, biological
              samples, and genetic information from thousands of participants
              and their families.
            </p>
          </ContentBlock>

          <ContentBlock id="general-design" title="General Design">
            <dl class="grid gap-4">
              <div
                v-for="col in designColumns"
                :key="col.id"
                class="flex gap-2"
              >
                <dt class="font-semibold min-w-40">{{ col.label }}:</dt>
                <dd>
                  <RecordColumn
                    :column="col"
                    :value="designRow[col.id]"
                    :get-ref-click-action="getRefClickAction"
                  />
                </dd>
              </div>
            </dl>
          </ContentBlock>

          <ContentBlock id="population" title="Population">
            <dl class="grid gap-4">
              <div
                v-for="col in populationColumns"
                :key="col.id"
                class="flex gap-2"
              >
                <dt class="font-semibold min-w-40">{{ col.label }}:</dt>
                <dd>
                  <RecordColumn
                    :column="col"
                    :value="populationRow[col.id]"
                    :get-ref-click-action="getRefClickAction"
                  />
                </dd>
              </div>
            </dl>
          </ContentBlock>

          <ContentBlock id="organisations" title="Organisations">
            <div class="grid gap-6">
              <div class="border border-gray-200 rounded p-4">
                <h3 class="font-semibold text-lg mb-2">
                  University Medical Center
                </h3>
                <p class="text-body-sm text-gray-600 mb-2">Lead organisation</p>
                <p class="text-body-base">
                  Primary institution responsible for data collection and
                  management.
                </p>
              </div>
              <div class="border border-gray-200 rounded p-4">
                <h3 class="font-semibold text-lg mb-2">Research Institute</h3>
                <p class="text-body-sm text-gray-600 mb-2">Partner</p>
                <p class="text-body-base">
                  Collaborating on genetic and biomarker analyses.
                </p>
              </div>
            </div>
          </ContentBlock>

          <ContentBlock id="publications" title="Publications">
            <ul class="list-disc list-inside space-y-2">
              <li>
                <a href="#" class="text-link hover:underline">
                  Study design and methodology paper (2020)
                </a>
              </li>
              <li>
                <a href="#" class="text-link hover:underline">
                  Longitudinal health outcomes analysis (2022)
                </a>
              </li>
              <li>
                <a href="#" class="text-link hover:underline">
                  Genetic markers and disease risk (2023)
                </a>
              </li>
            </ul>
          </ContentBlock>

          <ContentBlock
            id="access-conditions"
            title="Access Conditions"
            description="Data access is subject to approval by the data access committee."
          >
            <dl class="grid gap-4">
              <div
                v-for="col in accessColumns"
                :key="col.id"
                class="flex gap-2"
              >
                <dt class="font-semibold min-w-40">{{ col.label }}:</dt>
                <dd>
                  <RecordColumn
                    :column="col"
                    :value="accessRow[col.id]"
                    :get-ref-click-action="getRefClickAction"
                  />
                </dd>
              </div>
            </dl>
          </ContentBlock>
        </ContentBlocks>
      </template>
    </DetailPageLayout>
  </div>
</template>
