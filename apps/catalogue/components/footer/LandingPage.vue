<script lang="ts" setup>
import { useRoute } from "#app";
const route = useRoute();
</script>

<template>
  <footer class="bg-blue-700">
    <slot> </slot>
    <div class="grid grid-cols-1 lg:grid-cols-3 items-start p-6">
      <div class="text-white lg:col-span-2">
        <h3 class="uppercase mb-2 text-heading-lg font-display tracking-widest">
          Catalogues
        </h3>
        <NuxtLink :to="`/${route.params.schema}/catalogue/all`">
          <Button label="Search all" />
        </NuxtLink>
      </div>
      <div class="text-white lg:col-span-1">
        <h3 class="uppercase mb-2 text-heading-lg font-display tracking-widest">
          Created with MOLGENIS
        </h3>
        <ul class="list-style-none flex flex-col mb-5">
          <li>
            <a
              class="flex items-center hover:underline"
              href="http://molgenis.org"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>MOLGENIS.org</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li>
            <a
              class="flex items-center hover:underline"
              href="https://github.com/molgenis/molgenis-emx2"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>MOLGENIS on GitHub</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li class="flex items-ceter">
            <ClientOnly>
              <BaseIcon name="CaretRight" :width="16" />
              <FooterVersion />
            </ClientOnly>
          </li>
        </ul>
        <p class="mb-5">
          The MOLGENIS software in the European Health Research Data and Sample
          Catalogue is free to use under
          <a href="https://molgenis.org/LICENSE-LGPLv3.html" class="underline"
            >LGPLv3 License</a
          >. MOLGENIS adheres to the following
          <a
            href="https://molgenis.org/MOLGENIS_Privacy_Policy.pdf"
            class="underline"
            >Privacy Policy</a
          >.
        </p>
        <p>
          Need help?
          <a href="mailto:support@molgenis.org" class="underline">Contact us</a>
        </p>
      </div>
    </div>
    <div class="bg-blue-800 p-6">
      <div class="text-gray-400 text-body flex items-center">
        <span class="text-gray-200">On use, please cite:</span>
        <ul class="list-style-none ml-2 flex items-center gap-4">
          <li>
            <a
              class="hover:underline flex items-center"
              href="https://www.ncbi.nlm.nih.gov/pubmed/36463884"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Swertz & Gini (2022)</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li>
            <a
              class="hover:underline flex items-center"
              href="https://www.ncbi.nlm.nih.gov/pubmed/30165396"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Van der Velde et al (2018)</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li>
            <a
              class="hover:underline flex items-center"
              href="https://www.ncbi.nlm.nih.gov/pubmed/21210979"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Swertz et al (2010)</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
        </ul>
      </div>
    </div>
  </footer>
</template>
