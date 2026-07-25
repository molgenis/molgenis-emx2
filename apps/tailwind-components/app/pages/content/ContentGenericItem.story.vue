<script setup lang="ts">
import { ref } from "vue";
import type { ISectionField, cellPayload } from "../../../types/types";
import ContentGenericItem from "../../components/content/ContentGenericItem.vue";
import ContentGenericItemList from "../../components/content/ContentGenericItemList.vue";

const clickLog = ref<string[]>([]);

function logValueClick(payload: cellPayload) {
  clickLog.value.unshift(
    `${payload.metadata.id}: ${JSON.stringify(payload.data)}`
  );
}

function clearLog() {
  clickLog.value = [];
}

const contentTypeFields: ISectionField[] = [
  {
    meta: { id: "name", label: "Name", columnType: "STRING" },
    value: "Fluffy",
  },
  {
    meta: { id: "description", label: "Description", columnType: "TEXT" },
    value:
      "A friendly cat that has been with the shelter since 2021 and is looking for a quiet home.",
  },
  {
    meta: { id: "website", label: "Website", columnType: "HYPERLINK" },
    value: "https://molgenis.org",
  },
  {
    meta: { id: "tags", label: "Tags", columnType: "ONTOLOGY_ARRAY" },
    value: [
      { name: "friendly", label: "Friendly" },
      { name: "Trained" },
      { name: "vaccinated", label: "Vaccinated" },
    ],
  },
  {
    meta: {
      id: "visits",
      label: "Vet Visits",
      columnType: "REFBACK",
      refTableId: "Visit",
      refSchemaId: "pet store",
    },
    value: [
      { date: "2024-01-10", reason: "Annual checkup" },
      { date: "2024-04-05", reason: "Ear infection" },
    ],
  },
];

const previouslyBrokenFields: ISectionField[] = [
  {
    meta: { id: "species", label: "Species", columnType: "ONTOLOGY" },
    value: { name: "Dog", definition: "A domesticated carnivorous mammal" },
  },
  {
    meta: {
      id: "owner",
      label: "Owner",
      columnType: "REF",
      refTableId: "Owner",
      refSchemaId: "pet store",
      refLabel: "${firstName} ${lastName}",
      refLabelDefault: "${firstName}",
      refLinkId: "id",
    },
    value: { firstName: "John", lastName: "Doe", id: 1 },
  },
  {
    meta: {
      id: "vaccinations",
      label: "Vaccinations",
      columnType: "REF_ARRAY",
      refTableId: "Vaccination",
      refSchemaId: "pet store",
      refLabel: "${name}",
      refLabelDefault: "${name}",
      refLinkId: "id",
    },
    value: [
      { id: 1, name: "Rabies" },
      { id: 2, name: "Distemper" },
    ],
  },
  {
    meta: { id: "pedigree", label: "Pedigree", columnType: "FILE" },
    value: {
      id: "file-1",
      filename: "pedigree.pdf",
      extension: "pdf",
      url: "https://example.com/pedigree.pdf",
      size: 24576,
    },
  },
];

const fallbackTypeFields: ISectionField[] = [
  {
    meta: { id: "age", label: "Age", columnType: "INT" },
    value: 3,
  },
  {
    meta: { id: "neutered", label: "Neutered", columnType: "BOOL" },
    value: true,
  },
  {
    meta: { id: "birthDate", label: "Birth Date", columnType: "DATE" },
    value: "2021-06-15",
  },
  {
    meta: { id: "contact", label: "Contact", columnType: "EMAIL" },
    value: "shelter@example.com",
  },
];

const edgeCaseFields: ISectionField[] = [
  {
    meta: { id: "chipNumber", label: "", columnType: "STRING" },
    value: "528210000123456",
  },
  {
    meta: {
      id: "tags",
      label: "Tags (partly blank)",
      columnType: "ONTOLOGY_ARRAY",
    },
    value: [{ name: "Friendly" }, {}, { name: "" }, { name: "Trained" }],
  },
  {
    meta: {
      id: "tags",
      label: "Tags (not an array)",
      columnType: "ONTOLOGY_ARRAY",
    },
    value: "Friendly",
  },
  {
    meta: { id: "nickname", label: "Nickname", columnType: "STRING" },
    value: null,
  },
];

const spec = `
## Features
- Renders one label/value pair of a detail section as a \`dt\`/\`dd\` grid row
- Dispatches on \`field.meta.columnType\` to a ContentType* component
- Everything else falls through to ValueEMX2, the shared value dispatcher — so ONTOLOGY, REF, REF_ARRAY and FILE render properly instead of as object dumps
- Label falls back to the column id when no label is set
- Forwards \`valueClick\` from the value dispatcher so REF/ONTOLOGY links stay actionable

## Props
| Prop | Type | Default |
|------|------|---------|
| field | ISectionField | required |

## Emits
| Event | Payload |
|-------|---------|
| valueClick | cellPayload |

## Test Checklist
- [ ] Label shows the column label only — no columnType next to it
- [ ] Label falls back to the column id when label is missing (chipNumber)
- [ ] ONTOLOGY renders the term name, not \`[object Object]\`
- [ ] REF renders the refLabel template, not \`[object Object]\`
- [ ] REF_ARRAY renders a comma separated list of terms
- [ ] FILE renders a download link with a formatted size
- [ ] ONTOLOGY_ARRAY prefers label over name and skips blank terms
- [ ] ONTOLOGY_ARRAY with a non-array value renders empty, no error in console
- [ ] Clicking a REF or ONTOLOGY value appends to the click log
- [ ] Renders across all 5 themes (Light, Dark, Molgenis, UMCG, AUMC)
`;
</script>

<template>
  <Story title="ContentGenericItem" :spec="spec">
    <div class="space-y-8 py-4">
      <section class="space-y-2">
        <h2 class="text-heading-xl text-title-contrast">
          Dedicated ContentType components
        </h2>
        <div class="bg-content p-6 rounded shadow-primary">
          <ContentGenericItemList>
            <ContentGenericItem
              v-for="(field, index) in contentTypeFields"
              :key="`${field.meta.id}-${index}`"
              :field="field"
              @valueClick="logValueClick"
            />
          </ContentGenericItemList>
        </div>
      </section>

      <section class="space-y-2">
        <h2 class="text-heading-xl text-title-contrast">
          Routed to ValueEMX2 (previously object dumps)
        </h2>
        <div class="bg-content p-6 rounded shadow-primary">
          <ContentGenericItemList>
            <ContentGenericItem
              v-for="(field, index) in previouslyBrokenFields"
              :key="`${field.meta.id}-${index}`"
              :field="field"
              @valueClick="logValueClick"
            />
          </ContentGenericItemList>
        </div>
      </section>

      <section class="space-y-2">
        <h2 class="text-heading-xl text-title-contrast">
          Other types via ValueEMX2
        </h2>
        <div class="bg-content p-6 rounded shadow-primary">
          <ContentGenericItemList>
            <ContentGenericItem
              v-for="(field, index) in fallbackTypeFields"
              :key="`${field.meta.id}-${index}`"
              :field="field"
              @valueClick="logValueClick"
            />
          </ContentGenericItemList>
        </div>
      </section>

      <section class="space-y-2">
        <h2 class="text-heading-xl text-title-contrast">
          Missing label, blank terms, wrong value shape
        </h2>
        <div class="bg-content p-6 rounded shadow-primary">
          <ContentGenericItemList>
            <ContentGenericItem
              v-for="(field, index) in edgeCaseFields"
              :key="`${field.meta.id}-${index}`"
              :field="field"
              @valueClick="logValueClick"
            />
          </ContentGenericItemList>
        </div>
      </section>

      <section class="space-y-2">
        <div class="flex items-center justify-between">
          <h2 class="text-heading-xl text-title-contrast">Click event log</h2>
          <Button type="outline" size="small" label="Clear" @click="clearLog" />
        </div>
        <div class="bg-content p-6 rounded shadow-primary">
          <p v-if="clickLog.length === 0" class="text-disabled italic">
            No clicks yet
          </p>
          <ul v-else class="space-y-1 text-body-sm">
            <li v-for="entry in clickLog" :key="entry">{{ entry }}</li>
          </ul>
        </div>
      </section>
    </div>
  </Story>
</template>
