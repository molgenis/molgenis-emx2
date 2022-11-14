<script setup>
import Tooltip from "./Tooltip.vue";

const items = [
  { label: "Cohort type", content: "Population cohort, Birth cohort, Biobank" },
  { label: "CoDesign", content: "Cohort type", tooltip: "Tooltip content!" },
  { label: "Key", content: "Value" },
  { label: "Key", content: "Value" },
  { label: "Key", content: "Value" },
  {
    label: "Key",
    content:
      "Long value - Donec id elit non mi porta gravida at eget metus. Aenean lacinia bibendum nulla sed consectetur. Etiam porta sem malesuada magna mollis euismod. Nulla vitae elit libero, a pharetra augue.",
  },
  {
    label: "Age group at inclusion",
    content: [
      "Newborn (0-1 months)",
      "Newborn (0-1 months)",
      "Newborn (0-1 months)",
      "Newborn (0-1 months)",
      "Newborn (0-1 months)",
      "Newborn (0-1 months)",
    ],
  },
];

const isArray = (value) => {
  return Array.isArray(value);
};
</script>

<template>
  <dl class="grid gap-2.5 text-body-base text-gray-900">
    <div
      class="grid md:grid-cols-3 md:gap-2.5"
      v-for="item in items"
      :key="item.label"
    >
      <dt class="flex items-start font-bold text-body-base">
        <div class="flex items-center gap-1">
          {{ item.label }}
          <div v-if="item.tooltip">
            <Tooltip label="Lees meer" :content="item.tooltip" />
          </div>
        </div>
      </dt>

      <dd class="col-span-2">
        <ul
          v-if="isArray(item.content)"
          class="grid gap-1 pl-4 list-disc list-outside"
        >
          <li v-for="row in item.content" :key="row">
            {{ row }}
          </li>
        </ul>
        <p v-else>
          {{ item.content }}
        </p>
      </dd>
    </div>
  </dl>
</template>
