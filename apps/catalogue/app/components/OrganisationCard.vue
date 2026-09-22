<script setup lang="ts">
import { computed } from "vue";
import type { IOrganisations } from "../../interfaces/catalogue";

const props = defineProps<{
  organisation: IOrganisations;
}>();

const organisationName = computed(
  () =>
    props.organisation.organisation?.name ||
    props.organisation.otherOrganisation
);
</script>

<template>
  <li
    class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    data-component-name="name"
  >
    <div class="flex items-start flex-col h-full">
      <span class="block">
        <span class="font-bold" v-if="organisationName">
          {{ organisationName }}
          <template v-if="organisation?.organisation?.acronym">
            ({{ organisation.organisation.acronym }})
          </template>
        </span>
        <div v-if="organisation?.organisation?.country">
          {{ organisation?.organisation?.country.name }}
        </div>
      </span>
      <a
        class="text-link block hover:underline"
        v-if="organisation.organisation?.website"
        :href="organisation.organisation?.website"
      >
        {{ organisation.organisation?.website }}
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
