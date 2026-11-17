<script setup lang="ts">
import type { IOrganisationRoles } from "../../interfaces/catalogue";

defineProps<{
  organisation: IOrganisationRoles;
}>();
</script>

<template>
  <li
    class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    data-component-name="name"
  >
    <div class="flex items-start flex-col h-full">
      <span class="block">
        <span class="font-bold" v-if="organisation.organisationName">
          {{ organisation.organisationName }}
          <template v-if="organisation?.organisation?.acronym">
            ({{ organisation.organisation.acronym }})
          </template>
          <br />
          <span v-if="organisation?.department">
            {{ organisation?.department }}
          </span>
        </span>
        <div v-if="organisation?.organisation?.country">
          {{ organisation?.organisation?.country.name }}
        </div>
      </span>
      <a
        class="text-link block hover:underline"
        v-if="organisation.organisationWebsite"
        :href="organisation.organisationWebsite"
      >
        {{ organisation.organisationWebsite }}
      </a>

      <div v-if="organisation.role" class="mt-3">
        <p>
          <i>{{ organisation.role.map((r) => r.name).join(", ") }}</i>
        </p>
      </div>
    </div>
  </li>
</template>
