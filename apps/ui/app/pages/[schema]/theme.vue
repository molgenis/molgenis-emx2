<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import BreadCrumbs from "../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../tailwind-components/app/components/PageHeader.vue";
import Message from "../../../../tailwind-components/app/components/Message.vue";
import { useSession } from "../../../../tailwind-components/app/composables/useSession";
import ThemeManager from "../../components/theme/ThemeManager.vue";
import type { Crumb } from "../../../../tailwind-components/types/types";

const route = useRoute();
const schema = route.params.schema as string;

useHead({ title: `Theme - ${schema} - Molgenis` });

const crumbs: Crumb[] = [
  { label: schema, url: `/${schema}` },
  { label: "Theme", url: "" },
];

const { isAdmin, session } = await useSession(schema);
const canManage = computed(
  () => session.value?.roles?.[schema]?.includes("Manager") || isAdmin.value
);
</script>

<template>
  <Container>
    <PageHeader
      title="Theme"
      align="left"
      description="Choose how this database looks"
    >
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" align="left" />
      </template>
    </PageHeader>
    <ContentBlock v-if="canManage">
      <ThemeManager :schema="schema" />
    </ContentBlock>
    <Message v-else id="theme-permission-message" :invalid="true">
      You need at least the Manager role to change the theme of this database.
    </Message>
  </Container>
</template>
