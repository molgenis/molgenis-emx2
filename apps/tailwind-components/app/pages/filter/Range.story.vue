<template>
  <!--
    Story requirements (from plan v6.3.0):
    - [x] Renders min/max labels (defaults to 'Min'/'Max')
    - [x] Emits tuple on change
    - [x] Works with Input.vue in slots
    - [x] Mobile: stacks vertically on small screens (test at 375px)
    - [x] Supports INT, DATE, DECIMAL, DATETIME types

    ARIA accessibility:
    - [x] Uses <fieldset> for semantic grouping
    - [x] Uses <label for=""> for proper input association
    - [x] Optional sr-only <legend> for screen reader context
    - [x] Cursor pointer on labels for visual affordance
  -->
  <div class="space-y-8 p-6">
    <h1 class="text-heading-2xl font-bold">FilterRange Component</h1>

    <section class="space-y-4">
      <h2 class="text-heading-lg font-semibold">INT Type Range</h2>
      <p class="text-body-sm text-gray-600">
        Using Input.vue for integer range filter
      </p>

      <div class="max-w-md p-4 border rounded">
        <FilterRange v-model="intRange" id="age-filter">
          <template #min="{ value, update, id }">
            <Input
              :id="id"
              type="INT"
              :model-value="value"
              @update:model-value="update"
              placeholder="Min age"
            />
          </template>
          <template #max="{ value, update, id }">
            <Input
              :id="id"
              type="INT"
              :model-value="value"
              @update:model-value="update"
              placeholder="Max age"
            />
          </template>
        </FilterRange>

        <div class="mt-4 p-2 bg-gray-100 rounded text-body-sm">
          <strong>Current value:</strong> {{ JSON.stringify(intRange) }}
        </div>
      </div>
    </section>

    <section class="space-y-4">
      <h2 class="text-heading-lg font-semibold">DATE Type Range</h2>
      <p class="text-body-sm text-gray-600">
        Using Input.vue for date range filter
      </p>

      <div class="max-w-md p-4 border rounded">
        <FilterRange v-model="dateRange" id="date-filter">
          <template #min="{ value, update, id }">
            <Input
              :id="id"
              type="DATE"
              :model-value="value"
              @update:model-value="update"
              placeholder="Start date"
            />
          </template>
          <template #max="{ value, update, id }">
            <Input
              :id="id"
              type="DATE"
              :model-value="value"
              @update:model-value="update"
              placeholder="End date"
            />
          </template>
        </FilterRange>

        <div class="mt-4 p-2 bg-gray-100 rounded text-body-sm">
          <strong>Current value:</strong> {{ JSON.stringify(dateRange) }}
        </div>
      </div>
    </section>

    <section class="space-y-4">
      <h2 class="text-heading-lg font-semibold">DATETIME Type Range</h2>
      <p class="text-body-sm text-gray-600">
        Using Input.vue for datetime range filter
      </p>

      <div class="max-w-md p-4 border rounded">
        <FilterRange v-model="datetimeRange" id="datetime-filter">
          <template #min="{ value, update, id }">
            <Input
              :id="id"
              type="DATETIME"
              :model-value="value"
              @update:model-value="update"
              placeholder="Start datetime"
            />
          </template>
          <template #max="{ value, update, id }">
            <Input
              :id="id"
              type="DATETIME"
              :model-value="value"
              @update:model-value="update"
              placeholder="End datetime"
            />
          </template>
        </FilterRange>

        <div class="mt-4 p-2 bg-gray-100 rounded text-body-sm">
          <strong>Current value:</strong> {{ JSON.stringify(datetimeRange) }}
        </div>
      </div>
    </section>

    <section class="space-y-4">
      <h2 class="text-heading-lg font-semibold">Custom Labels</h2>
      <p class="text-body-sm text-gray-600">Using custom labels "From"/"To"</p>

      <div class="max-w-md p-4 border rounded">
        <FilterRange
          v-model="customRange"
          id="price-filter"
          min-label="From"
          max-label="To"
        >
          <template #min="{ value, update, id }">
            <Input
              :id="id"
              type="DECIMAL"
              :model-value="value"
              @update:model-value="update"
              placeholder="0.00"
            />
          </template>
          <template #max="{ value, update, id }">
            <Input
              :id="id"
              type="DECIMAL"
              :model-value="value"
              @update:model-value="update"
              placeholder="0.00"
            />
          </template>
        </FilterRange>

        <div class="mt-4 p-2 bg-gray-100 rounded text-body-sm">
          <strong>Current value:</strong> {{ JSON.stringify(customRange) }}
        </div>
      </div>
    </section>

    <section class="space-y-4">
      <h2 class="text-heading-lg font-semibold">With Legend (Accessibility)</h2>
      <p class="text-body-sm text-gray-600">
        Using legend prop for screen reader context (sr-only, not visible)
      </p>

      <div class="max-w-md p-4 border rounded">
        <FilterRange
          v-model="legendRange"
          id="year-filter"
          legend="Filter by publication year"
        >
          <template #min="{ value, update, id }">
            <Input
              :id="id"
              type="INT"
              :model-value="value"
              @update:model-value="update"
              placeholder="From year"
            />
          </template>
          <template #max="{ value, update, id }">
            <Input
              :id="id"
              type="INT"
              :model-value="value"
              @update:model-value="update"
              placeholder="To year"
            />
          </template>
        </FilterRange>

        <div class="mt-4 p-2 bg-gray-100 rounded text-body-sm">
          <strong>Current value:</strong> {{ JSON.stringify(legendRange) }}
          <br />
          <em class="text-gray-500">
            (Inspect DOM to see &lt;legend class="sr-only"&gt;)
          </em>
        </div>
      </div>
    </section>

    <section class="space-y-4">
      <h2 class="text-heading-lg font-semibold">Mobile Test (375px)</h2>
      <p class="text-body-sm text-gray-600">
        Component should stack vertically on small screens
      </p>

      <div class="max-w-[375px] p-4 border rounded">
        <FilterRange v-model="mobileRange" id="mobile-filter">
          <template #min="{ value, update, id }">
            <Input
              :id="id"
              type="INT"
              :model-value="value"
              @update:model-value="update"
              placeholder="Min"
            />
          </template>
          <template #max="{ value, update, id }">
            <Input
              :id="id"
              type="INT"
              :model-value="value"
              @update:model-value="update"
              placeholder="Max"
            />
          </template>
        </FilterRange>

        <div class="mt-4 p-2 bg-gray-100 rounded text-body-sm">
          <strong>Current value:</strong> {{ JSON.stringify(mobileRange) }}
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import FilterRange from "../../components/filter/Range.vue";
import Input from "../../components/Input.vue";

const intRange = ref<[any, any]>([null, null]);
const dateRange = ref<[any, any]>([null, null]);
const datetimeRange = ref<[any, any]>([null, null]);
const customRange = ref<[any, any]>([null, null]);
const legendRange = ref<[any, any]>([null, null]);
const mobileRange = ref<[any, any]>([null, null]);
</script>
