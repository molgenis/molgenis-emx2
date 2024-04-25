<script setup lang="ts">
import type { IContributor } from "~/interfaces/types";

defineProps<{
  title: string;
  description?: string;
  contributors: IContributor[];
}>();
</script>

<template>
  <ContentBlock :title="title">
    <slot></slot>
    <ContactCardList>
      <ContactCard v-for="contributor in contributors" :contact="contributor" />
    </ContactCardList>
  </ContentBlock>
</template>
