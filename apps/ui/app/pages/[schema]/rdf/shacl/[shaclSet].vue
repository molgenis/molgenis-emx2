<script lang="ts" setup>
import { useRoute } from "vue-router";
import { useHead, useState } from "#app";
import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import CustomTooltip from "../../../../../../tailwind-components/app/components/CustomTooltip.vue";
import ContentBasic from "../../../../../../tailwind-components/app/components/content/ContentBasic.vue";
import LoadingContent from "../../../../../../tailwind-components/app/components/LoadingContent.vue";
import Button from "../../../../../../tailwind-components/app/components/Button.vue";
import ButtonDownloadBlob from "../../../../../../tailwind-components/app/components/button/DownloadBlob.vue";
import DisplayCodeBlock from "../../../../../../tailwind-components/app/components/display/CodeBlock.vue";
import type { ProcessData } from "../../../../../../metadata-utils/src/generic";
import IconProcess from "../../../../../../tailwind-components/app/components/icon/Process.vue";

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

// Structure: routeSchema -> routeShaclSet -> ProcessData
const shaclSetRuns = useState("shaclSetRuns",
  () => ({} as Record<string, Record<string, ProcessData>>)
);

function getProcessData(): ProcessData {
  if (!shaclSetRuns.value[routeSchema]) {
    shaclSetRuns.value[routeSchema] = {}
  }
  if(!shaclSetRuns.value[routeSchema][routeShaclSet]) {
    shaclSetRuns.value[routeSchema][routeShaclSet] = {status: "UNKNOWN"};
  }
  return shaclSetRuns.value[routeSchema][routeShaclSet];
}

async function runShacl() {
  if (processData.status === "RUNNING") return;

  processData.output = undefined;
  processData.error = undefined;
  processData.status = "RUNNING";

  const res = await fetch(`/${routeSchema}/api/rdf?validate=${routeShaclSet}`);
  processData.output = await res.text();

  if (res.status !== 200) {
    processData.status = "ERROR";
    processData.error = `Error (status code: ${res.status})`;
  } else if (validateShaclOutput(processData.output)) {
    processData.status = "DONE";
  } else {
    processData.status = "INVALID";
  }
}

function validateShaclOutput(output: string): boolean {
  return output
      .substring(0, 100)
      .includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.");
}

const processData = getProcessData();
if(processData.status === "UNKNOWN") runShacl();
</script>

<template>
  <Container>
    <PageHeader :title="`${routeShaclSet} for ${routeSchema}`" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
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
        <Button
            type="outline"
            size="small"
            icon="refresh"
            :iconOnly="true"
            label="rerun"
            :disabled="processData.status === 'RUNNING'"
            @click.prevent="runShacl"
        />
        <ButtonDownloadBlob
            size="small"
            :iconOnly="true"
            :data="processData.output"
            mediaType="text/turtle"
            :fileName="`${routeSchema} - shacl - ${routeShaclSet}.ttl`"
        />
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
