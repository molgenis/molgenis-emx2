<template>
  <div>
    <div class="flex flex-wrap gap-2 p-4 bg-gray-100 rounded mb-4">
      <NuxtLink
        v-for="resource in resources"
        :key="resource.id"
        :to="`/samples/catalogue/${resource.id}`"
        class="px-3 py-1 rounded text-sm"
        :class="
          resource.id === route.params.resource
            ? 'bg-blue-600 text-white'
            : 'bg-white text-blue-600 hover:bg-blue-50'
        "
      >
        {{ resource.acronym || resource.id }}
      </NuxtLink>
    </div>
    <DetailView
      schema-id="catalogue-demo"
      table-id="Resources"
      :row-id="rowId"
      :show-side-nav="true"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useAsyncData } from "#app";
import DetailView from "../../../components/display/DetailView.vue";
import fetchGraphql from "../../../composables/fetchGraphql";

const route = useRoute();

const rowId = computed(() => ({
  id: route.params.resource as string,
}));

const { data: resourceList } = useAsyncData("catalogue-resources", () =>
  fetchGraphql("catalogue-demo", `{ Resources { id, acronym } }`, undefined)
);

const resources = computed(() => resourceList.value?.Resources || []);
</script>
