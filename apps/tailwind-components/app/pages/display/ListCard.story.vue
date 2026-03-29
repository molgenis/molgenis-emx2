<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import ListCard from "../../components/display/ListCard.vue";

const columns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING", key: 1 },
  { id: "description", label: "Description", columnType: "TEXT", key: 0 },
  { id: "type", label: "Type", columnType: "STRING", key: 0 },
  { id: "contact", label: "Contact", columnType: "STRING", key: 0 },
];

const sampleData = {
  name: "Biobank Amsterdam",
  description: "A large cohort of patients in Amsterdam.",
  type: "Population-based",
  contact: "info@biobank-ams.nl",
};

const dataWithRef = {
  name: "Genomics Study",
  description: "Genomics research cohort",
  type: { name: "Observational" },
  contact: null,
};

const dataWithArrayRef = {
  name: "Multi-center Trial",
  description: null,
  type: "Interventional",
  contact: [{ name: "Alice" }, { name: "Bob" }],
};

const minimalData = {
  name: "Simple Entry",
};
</script>

<template>
  <Story
    title="ListCard"
    description="Generic card component for displaying summary fields of a record as label-value pairs with optional detail link."
  >
    <div class="p-5 space-y-6">
      <div
        class="bg-content p-6 rounded shadow-primary space-y-6 text-record-value"
      >
        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            With all fields
          </h2>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <ListCard
              title="Biobank Amsterdam"
              :data="sampleData"
              :columns="columns"
              href="/testSchema/Cohort/Biobank%20Amsterdam"
            />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            With object and array ref values
          </h2>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <ListCard
              title="Genomics Study"
              :data="dataWithRef"
              :columns="columns"
              href="/testSchema/Cohort/Genomics%20Study"
            />
            <ListCard
              title="Multi-center Trial"
              :data="dataWithArrayRef"
              :columns="columns"
              href="/testSchema/Cohort/Multi-center%20Trial"
            />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            Without columns (no dl rendered)
          </h2>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <ListCard title="Simple Entry" :data="minimalData" />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            Without href (no link)
          </h2>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <ListCard
              title="Biobank Amsterdam"
              :data="sampleData"
              :columns="columns"
            />
          </div>
        </div>
      </div>
    </div>
  </Story>
</template>
