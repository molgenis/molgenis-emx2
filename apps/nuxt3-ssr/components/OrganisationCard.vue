<script setup lang="ts">
defineProps<{
  organisation: IOrganisation;
}>();
</script>

<template>
  <li
    class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    data-component-name="name"
  >
    <!--
    <IconButton
      label="label"
      icon="star"
      class="text-blue-500 absolute right-2 top-2"
    />
  -->
    <div class="flex items-start justify-center flex-col h-full">
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
    </div>
  </li>
</template>
