<script lang="ts" setup>
import { useRoute } from "vue-router";
import {useHead, useState} from "#app";
import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import CustomTooltip from "../../../../../../tailwind-components/app/components/CustomTooltip.vue";
import ContentBasic from "../../../../../../tailwind-components/app/components/content/ContentBasic.vue";
import LoadingContent from "../../../../../../tailwind-components/app/components/LoadingContent.vue";
import Button from "../../../../../../tailwind-components/app/components/Button.vue";
import ButtonDownloadBlob from "../../../../../../tailwind-components/app/components/button/DownloadBlob.vue";
import DisplayCodeBlock from "../../../../../../tailwind-components/app/components/display/CodeBlock.vue";
import type {ProcessData} from "../../../../../../metadata-utils/src/generic";
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

function validateShaclOutput(output: string): boolean {
  return output
    .substring(0, 100)
    .includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.");
}

const shaclSetRuns = useState(`${routeSchema}-shaclSetRuns`, () => ({} as Record<string, ProcessData>));
if(!shaclSetRuns.value[routeShaclSet]) {
  shaclSetRuns.value[routeShaclSet] = {status: "UNKNOWN"}
  runShacl();
}

async function runShacl() {
  if(shaclSetRuns.value[routeShaclSet].status === "RUNNING") return;

  shaclSetRuns.value[routeShaclSet].output = undefined;
  shaclSetRuns.value[routeShaclSet].error = undefined;
  shaclSetRuns.value[routeShaclSet].status = "RUNNING";

  const res = await fetch(`/${routeSchema}/api/rdf?validate=${routeShaclSet}`);
  shaclSetRuns.value[routeShaclSet].output = await res.text();

  if (res.status !== 200) {
    shaclSetRuns.value[routeShaclSet].status = "ERROR";
    shaclSetRuns.value[routeShaclSet].error = `Error (status code: ${res.status})`;
  } else if (validateShaclOutput(shaclSetRuns.value[routeShaclSet].output)) {
    shaclSetRuns.value[routeShaclSet].status = "DONE";
  } else {
    shaclSetRuns.value[routeShaclSet].status = "INVALID";
  }
}
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
      <IconProcess :status="shaclSetRuns[routeShaclSet].status" />
      <Button
          type="primary"
          size="small"
          label="rerun"
          :disabled="shaclSetRuns[routeShaclSet].status === 'RUNNING'"
          @click.prevent="runShacl"
      />
      <ButtonDownloadBlob
          size="small"
          :data="shaclSetRuns[routeShaclSet].output"
          mediaType="text/turtle"
          :disabled="!shaclSetRuns[routeShaclSet].output"
          :fileName="`${routeSchema} - shacl - ${routeShaclSet}.ttl`"
      />
      <LoadingContent :id="`shaclSet-${routeShaclSet}`" :status="'success'" loading-text="Running validation" error-text="Failed to run validation">
        <DisplayCodeBlock :content="shaclSetRuns[routeShaclSet].output" />
      </LoadingContent>
    </ContentBasic>
  </Container>
</template>
