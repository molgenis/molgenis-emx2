<script setup>
defineProps({
  contact: {
    type: Object,
  },
  contributionDescription: {
    type: String,
  },
});
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
        <span class="font-bold block" v-if="contact?.title">
          {{ contact?.title.name }}
        </span>
        <span v-if="contact?.initials"> {{ contact?.initials }} </span>
        <span v-if="contact?.firstName && contact?.initials">
          ({{ contact?.firstName }})
        </span>
        <span v-else-if="contact?.firstName">
          {{ contact?.firstName }}&nbsp;</span
        >
        <span v-if="contact?.surname"> {{ contact?.surname }} </span>
      </span>
      <a
        class="text-blue-500 block hover:underline"
        v-if="contact?.email"
        :href="`mailto:${contact?.email}`"
      >
        {{ contact?.email }}
      </a>
      <div v-if="contributionDescription" class="mt-3">
        <p>{{ contributionDescription }}</p>
      </div>
      <div v-if="contact?.department" class="mt-3">
        <p>{{ contact?.department }}</p>
      </div>
    </div>
  </li>
</template>
