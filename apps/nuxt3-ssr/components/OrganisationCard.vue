<script setup lang="ts">
import type { IOrganisations } from "~/interfaces/catalogue";

defineProps<{
  organisation: IOrganisations;
}>();
</script>

<template>
  <li
    class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    data-component-name="name"
  >
    <div class="flex items-start flex-col h-full">
      <span class="block">
        <span class="font-bold" v-if="organisation?.name">
          {{ organisation?.name }}&nbsp;<template v-if="organisation.acronym"
            >({{ organisation.acronym }})</template
          >
        </span>
        <div v-if="organisation.country">
          {{ organisation.country.map((r) => r.name).join(", ") }}
        </div>
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

      <!-- todo: decide how we will bring back logo <img
        v-if="organisation.logo"
        class="max-h-11"
        :src="organisation.logo.url"
      /-->
    </div>
  </li>
</template>
