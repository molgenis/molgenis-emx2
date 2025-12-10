<script lang="ts" setup>
import { useRoute } from "vue-router";
import { useHead } from "#app";
import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import CustomTooltip from "../../../../../../tailwind-components/app/components/CustomTooltip.vue";
import ContentBasic from "../../../../../../tailwind-components/app/components/content/ContentBasic.vue";
import LoadingContent from "../../../../../../tailwind-components/app/components/LoadingContent.vue";
import DisplayCodeBlock from "../../../../../../tailwind-components/app/components/display/CodeBlock.vue";
import {navigateTo} from "nuxt/app";
import {
  downloadShacl
} from "../../../../../../tailwind-components/app/utils/downloadBlob";
import Button from "../../../../../../tailwind-components/app/components/Button.vue";
import {getProcessData, runShacl} from "../../../../util/shaclUtils";

const route = useRoute();
const routeSchema = (
  Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema
) as string;

const routeShaclSet = (
  Array.isArray(route.params.shaclSet)
    ? route.params.shaclSet
    : route.params.shaclSet
) as string;

useHead({
  title: `${routeShaclSet} - SHACL - RDF - ${routeSchema}  - Molgenis`,
});

const crumbs: Record<string, string> = {};
crumbs[routeSchema] = `/${routeSchema}`;
crumbs["rdf"] = `/${routeSchema}/rdf`;
crumbs["shacl"] = `/${routeSchema}/rdf/shacl`;
crumbs[`${routeShaclSet}`] = "";

const processData = getProcessData(routeSchema, routeShaclSet);
if(processData.status === "UNKNOWN") runShacl(processData, routeSchema, routeShaclSet);
</script>

<template>
  <Container>
    <PageHeader :title="`${routeShaclSet} validation of ${routeSchema}`" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
      <template #title-prefix>
        <Button
            class="mr-4"
            type="filterWell"
            size="large"
            :iconOnly="true"
            icon="arrow-left"
            @click="navigateTo(crumbs['shacl'])"
        />
      </template>
      <template #description>
        Validate the RDF API output for the complete schema.
        <CustomTooltip
          label=""
          content="Output is deemed valid if nodes adhere to the requirements or those nodes are not present."
        />
      </template>
    </PageHeader>
    <ContentBasic>
      <template #controls>
        <Button type="outline" size="small" label="run" icon="playArrow" :disabled="processData.status === 'RUNNING'" @click.prevent="runShacl(processData, routeSchema, routeShaclSet)" />
        <Button type="outline" size="small" label="download" icon="download" :disabled="!processData.output" @click.prevent="downloadShacl(processData.output, routeSchema, routeShaclSet)" />
      </template>
      <LoadingContent
        :id="`shaclSet-${routeShaclSet}`"
        :status="processData.status"
        loading-text="Running validation (this might take a while)"
        :error-text="processData.error"
      >
        <DisplayCodeBlock :content="processData.output" />
      </LoadingContent>
    </ContentBasic>
  </Container>
</template>
