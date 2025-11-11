<script lang="ts" setup>
import { useRoute } from "vue-router";
import {useAsyncData, useHead, useState} from "#app";
import type {
  ShaclSet,
  ShaclSetRun,
  ShaclSetValidation
} from "../../../../../../metadata-utils/src/rdf";
import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import CustomTooltip from "../../../../../../tailwind-components/app/components/CustomTooltip.vue";
import ContentBasic from "../../../../../../tailwind-components/app/components/content/ContentBasic.vue";
import LoadingContent from "../../../../../../tailwind-components/app/components/LoadingContent.vue";
import BaseIcon from "../../../../../../tailwind-components/app/components/BaseIcon.vue";
import Message from "../../../../../../tailwind-components/app/components/Message.vue";
import Button from "../../../../../../tailwind-components/app/components/Button.vue";
import ButtonDownloadBlob from "../../../../../../tailwind-components/app/components/button/DownloadBlob.vue";
import DisplayCodeBlock from "../../../../../../tailwind-components/app/components/display/CodeBlock.vue";
import {useFetch} from "#app/composables/fetch";
import type {ProcessData, ProcessStatus} from "metadata-utils/src/generic";

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

const shaclSetRun = useState<ProcessData>(`${routeSchema}_shaclSetRun_${routeShaclSet}`, () => ({status: "UNKNOWN"}));

async function runShacl() {
  shaclSetRun.value.status = "RUNNING";
  const res = await fetch(`/${routeSchema}/api/rdf?validate=${routeShaclSet}`);
  shaclSetRun.value.output = await res.text();

  if (res.status !== 200) {
    shaclSetRun.value.status = "ERROR";
    shaclSetRun.value.error = `Error (status code: ${res.status})`;
  } else if (validateShaclOutput(shaclSetRun.value.output)) {
    shaclSetRun.value.status = "DONE";
  } else {
    shaclSetRun.value.status = "INVALID";
  }
}

if(shaclSetRun.value.status === "UNKNOWN") runShacl();
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
      <Button
          type="primary"
          size="small"
          label="validate"
          @click.prevent="runShacl"
      />
      <ButtonDownloadBlob
          size="small"
          :data="shaclSetRun.output"
          mediaType="text/turtle"
          :fileName="`${routeSchema} - shacl - ${routeShaclSet}.ttl`"
      />
      <LoadingContent :id="`shaclSet-${routeShaclSet}`" :status="'success'" loading-text="Running validation" error-text="Failed to run validation">
        <DisplayCodeBlock :content="shaclSetRun.output" />
      </LoadingContent>
    </ContentBasic>
                <!--        <div class="flex flex-col gap-2.5 items-start md:flex-row">-->
    <!--          <div class="flex items-baseline gap-5">-->
    <!--            <Button-->
    <!--                class="flex-none"-->
    <!--                type="outline"-->
    <!--                size="small"-->
    <!--                icon="caretLeft"-->
    <!--                label="go back"-->
    <!--                @click.prevent="toggleShaclOutputView(shaclSet)"-->
    <!--            />-->
    <!--            <h3 class="uppercase text-heading-4xl font-display">-->
    <!--              {{ shaclSet.name }} (version: {{ shaclSet.version }})-->
    <!--            </h3>-->
    <!--          </div>-->
    <!--          <div class="flex items-center gap-2.5 ml-auto">-->
    <!--            <BaseIcon-->
    <!--                name="progress-activity"-->
    <!--                class="animate-spin flex-none"-->
    <!--                :width="30"-->
    <!--                v-if="shaclSet.status === 'RUNNING'"-->
    <!--            />-->
    <!--            <BaseIcon-->
    <!--                name="check"-->
    <!--                class="flex-none"-->
    <!--                :width="30"-->
    <!--                v-else-if="shaclSet.status === 'VALID'"-->
    <!--            />-->
    <!--            <BaseIcon-->
    <!--                name="cross"-->
    <!--                class="flex-none"-->
    <!--                :width="30"-->
    <!--                v-else-if="shaclSet.status === 'INVALID'"-->
    <!--            />-->
    <!--            <BaseIcon-->
    <!--                name="exclamation"-->
    <!--                class="flex-none"-->
    <!--                :width="30"-->
    <!--                v-else-if="shaclSet.status === 'ERROR'"-->
    <!--            />-->
    <!--            <Button-->
    <!--                type="primary"-->
    <!--                size="small"-->
    <!--                label="validate"-->
    <!--                :disabled="shaclSet.status === 'RUNNING'"-->
    <!--                @click.prevent="runShacl(shaclSet)"-->
    <!--            />-->
    <!--            <ButtonDownloadBlob-->
    <!--                size="small"-->
    <!--                :disabled="-->
    <!--                    shaclSet.status !== 'VALID' && shaclSet.status !== 'INVALID'-->
    <!--                  "-->
    <!--                :data="shaclSet.output"-->
    <!--                mediaType="text/turtle"-->
    <!--                :fileName="`${schema} - shacl - ${shaclSet.id}.ttl`"-->
    <!--            />-->
    <!--          </div>-->
    <!--        </div>-->
    <!--        <Message-->
    <!--            :id="`shacl-validation-${shaclSet.id}-error`"-->
    <!--            class="my-8"-->
    <!--            :invalid="true"-->
    <!--            v-if="shaclSet.error"-->
    <!--        >-->
    <!--          <span>{{ shaclSet.error }}</span>-->
    <!--        </Message>-->
    <!--        <DisplayOutput class="px-8 my-8 overflow-x-auto">-->
    <!--          <pre>{{ shaclSet.output }}</pre>-->
    <!--        </DisplayOutput>-->
  </Container>
</template>
