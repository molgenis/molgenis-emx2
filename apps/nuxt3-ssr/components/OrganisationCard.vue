<script setup lang="ts">
import type { IOrganisation } from "~/interfaces/types";

defineProps<{
  organisation: IOrganisation;
}>();
</script>

<template>
  <li
    class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    data-component-name="name"
  >
    <div class="flex items-start justify-center flex-col h-full">
      <div
        v-if="organisation.isLeadOrganisation"
        class="font-bold text-body-base uppercase py-3"
      >
        Lead organisation
      </div>

      <span class="font-bold block">
        <span class="font-bold" v-if="organisation?.name">
          {{ organisation?.name }}&nbsp;<template v-if="organisation.acronym"
            >({{ organisation.acronym }})</template
          >
        </span>
      </span>
      <a
        class="text-blue-500 block hover:underline"
        v-if="organisation.website"
        :href="organisation.website"
      >
        {{ organisation.website }}
      </a>

      <div v-if="organisation.role" class="mt-3">
        <p>
          <i>{{ organisation.role.map((r) => r.name).join(", ") }}</i>
        </p>
      </div>

      <div v-if="organisation.country" class="mt-3">
        <p>
          <i>{{ organisation.country.map((r) => r.name).join(", ") }}</i>
        </p>
      </div>

      <img
        v-if="organisation.logo"
        class="max-h-11"
        :src="organisation.logo.url"
      />
    </div>
  </li>
</template>
