<script setup lang="ts">
import { computed } from "vue";
import type { IContacts } from "../../../interfaces/catalogue";
import ContactCard from "../ContactCard.vue";
import ContactCardList from "../ContactCardList.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";

const props = defineProps<{
  title: string;
  description?: string;
  contributors: IContacts[];
}>();

const orderedRoles = computed(() => {
  return props.contributors?.sort((a, b) => {
    const primaryRoleA =
      a?.role?.length && a.role[0] ? a.role[0].order ?? -1 : -1;
    const primaryRoleB =
      b?.role?.length && b.role[0] ? b.role[0].order ?? -1 : -1;
    return primaryRoleA - primaryRoleB;
  });
});
</script>

<template>
  <ContentBlock :title="title">
    <slot name="before"></slot>

    <template v-if="orderedRoles?.length > 0">
      <ContactCardList>
        <ContactCard
          v-for="contributor in orderedRoles"
          :contact="contributor"
        />
      </ContactCardList>
    </template>

    <slot name="after"></slot>
  </ContentBlock>
</template>
