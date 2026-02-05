<template>
  <div class="p-6">
    <h1 class="text-heading-2xl font-bold mb-6">FilterColumn Component</h1>

    <div class="flex gap-4 items-center mb-6 flex-wrap">
      <div class="flex gap-2 items-center">
        <label class="font-semibold">Background:</label>
        <button
          @click="useDarkBg = true"
          :class="[
            'px-3 py-1 rounded',
            useDarkBg ? 'bg-blue-500 text-white' : 'bg-gray-200',
          ]"
        >
          Sidebar (dark)
        </button>
        <button
          @click="useDarkBg = false"
          :class="[
            'px-3 py-1 rounded',
            !useDarkBg ? 'bg-blue-500 text-white' : 'bg-gray-200',
          ]"
        >
          Form (light)
        </button>
      </div>
      <div class="flex gap-2 items-center">
        <label class="font-semibold">Mobile:</label>
        <button
          @click="isMobile = !isMobile"
          :class="[
            'px-3 py-1 rounded',
            isMobile ? 'bg-blue-500 text-white' : 'bg-gray-200',
          ]"
        >
          {{ isMobile ? "On" : "Off" }}
        </button>
      </div>
    </div>

    <div
      class="p-4 rounded-t-3px rounded-b-50px"
      :class="[
        useDarkBg ? 'bg-sidebar-gradient' : 'bg-white border',
        isMobile ? 'max-w-[375px]' : 'max-w-md',
      ]"
    >
      <h2
        v-if="!isMobile"
        class="p-5 uppercase font-display text-heading-3xl text-search-filter-title"
      >
        Filters
      </h2>

      <FilterColumn
        :column="stringColumn"
        v-model="stringFilter"
        :mobile-display="isMobile"
      />
      <FilterColumn
        :column="intColumn"
        v-model="intFilter"
        :mobile-display="isMobile"
      />
      <FilterColumn
        :column="decimalColumn"
        v-model="decimalFilter"
        :mobile-display="isMobile"
      />
      <FilterColumn
        :column="dateColumn"
        v-model="dateFilter"
        :mobile-display="isMobile"
      />
      <FilterColumn
        :column="boolColumn"
        v-model="boolFilter"
        :mobile-display="isMobile"
      />
      <FilterColumn
        :column="refColumn"
        v-model="refFilter"
        :mobile-display="isMobile"
      />
      <FilterColumn
        :column="ontologyColumn"
        v-model="ontologyFilter"
        :mobile-display="isMobile"
      />
      <FilterColumn
        :column="ontologyArrayColumn"
        v-model="ontologyArrayFilter"
        :mobile-display="isMobile"
      />

      <hr class="mx-5 border-black opacity-10" />
    </div>

    <div class="mt-4 p-4 bg-white rounded border max-w-md">
      <h3 class="font-semibold mb-2">Current Filter Values</h3>
      <div class="text-body-sm space-y-1 font-mono">
        <div><strong>A String:</strong> {{ JSON.stringify(stringFilter) }}</div>
        <div><strong>An Int:</strong> {{ JSON.stringify(intFilter) }}</div>
        <div>
          <strong>A Decimal:</strong> {{ JSON.stringify(decimalFilter) }}
        </div>
        <div><strong>A Date:</strong> {{ JSON.stringify(dateFilter) }}</div>
        <div><strong>A Boolean:</strong> {{ JSON.stringify(boolFilter) }}</div>
        <div><strong>A Ref:</strong> {{ JSON.stringify(refFilter) }}</div>
        <div>
          <strong>An Ontology:</strong> {{ JSON.stringify(ontologyFilter) }}
        </div>
        <div>
          <strong>Ontology Array:</strong>
          {{ JSON.stringify(ontologyArrayFilter) }}
        </div>
      </div>
    </div>

    <section class="mt-12 p-6 bg-gray-50 rounded border max-w-2xl">
      <h2 class="text-heading-lg font-semibold mb-4">Specification</h2>
      <div class="text-body-sm space-y-4">
        <p>
          Collapsible filter component for sidebar use. Automatically renders
          appropriate input based on column type.
        </p>

        <h3 class="font-semibold">Props</h3>
        <ul class="list-disc pl-5">
          <li><code>column: IColumn</code> - Column metadata (required)</li>
          <li>
            <code>modelValue: IFilterValue | null</code> - v-model filter value
          </li>
          <li>
            <code>collapsed: boolean</code> - Initial collapse state (default:
            true)
          </li>
          <li>
            <code>mobileDisplay: boolean</code> - Mobile styling variant
            (default: false)
          </li>
        </ul>

        <h3 class="font-semibold">Column Type → Input Mapping</h3>
        <ul class="list-disc pl-5">
          <li>STRING, TEXT, EMAIL → text input (like operator)</li>
          <li>INT, DECIMAL, LONG, DATE, DATETIME → min/max range (between)</li>
          <li>BOOL → yes/no toggle (equals)</li>
          <li>REF, REF_ARRAY → dropdown selector (in)</li>
          <li>ONTOLOGY, ONTOLOGY_ARRAY → tree selector (in)</li>
        </ul>

        <h3 class="font-semibold">Test Checklist</h3>
        <ul class="list-disc pl-5">
          <li>Click title to collapse/expand - caret should rotate</li>
          <li>Enter value - Clear button should appear</li>
          <li>Click Clear - value should reset to null</li>
          <li>Toggle Background button - verify readability on both</li>
          <li>Toggle Mobile button - check 375px width styling</li>
          <li>
            Switch to AUMC theme - check Yes/No and Min/Max labels visible
          </li>
        </ul>

        <h3 class="font-semibold">Known Issues</h3>
        <ul class="list-disc pl-5">
          <li>
            ⚠️ Boolean Yes/No labels may not be visible on dark backgrounds in
            some themes (AUMC). Needs
            <code>inverted</code> prop on Input components.
          </li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "../../components/filter/Column.vue";

const useDarkBg = ref(true);
const isMobile = ref(false);

const stringColumn: IColumn = {
  id: "name",
  label: "A String",
  columnType: "STRING",
};

const intColumn: IColumn = {
  id: "age",
  label: "An Int",
  columnType: "INT",
};

const decimalColumn: IColumn = {
  id: "price",
  label: "A Decimal",
  columnType: "DECIMAL",
};

const dateColumn: IColumn = {
  id: "birthdate",
  label: "A Date",
  columnType: "DATE",
};

const boolColumn: IColumn = {
  id: "active",
  label: "A Boolean",
  columnType: "BOOL",
};

const refColumn: IColumn = {
  id: "pet",
  label: "A Ref",
  columnType: "REF",
  refSchemaId: "pet store",
  refTableId: "Pet",
  refLabel: "${name}",
};

const ontologyColumn: IColumn = {
  id: "country",
  label: "An Ontology",
  columnType: "ONTOLOGY",
  refSchemaId: "CatalogueOntologies",
  refTableId: "Countries",
};

const ontologyArrayColumn: IColumn = {
  id: "keywords",
  label: "Ontology Array",
  columnType: "ONTOLOGY_ARRAY",
  refSchemaId: "CatalogueOntologies",
  refTableId: "Keywords",
};

const stringFilter = ref<IFilterValue | null>(null);
const intFilter = ref<IFilterValue | null>(null);
const decimalFilter = ref<IFilterValue | null>(null);
const dateFilter = ref<IFilterValue | null>(null);
const boolFilter = ref<IFilterValue | null>(null);
const refFilter = ref<IFilterValue | null>(null);
const ontologyFilter = ref<IFilterValue | null>(null);
const ontologyArrayFilter = ref<IFilterValue | null>(null);
</script>
