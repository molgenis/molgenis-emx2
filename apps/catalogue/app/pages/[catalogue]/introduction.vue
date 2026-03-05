<script setup lang="ts">
import PageHeader from "../../../../tailwind-components/app/components/PageHeader.vue";
import LayoutsDetailPage from "../../components/layouts/DetailPage.vue";
import SideNavigation from "../../components/SideNavigation.vue";
import ContentBlocks from "../../../../tailwind-components/app/components/content/ContentBlocks.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";

import { useHead } from "#app";

useHead({
  title: "Introduction",
  meta: [
    {
      name: "description",
      content:
        "Introduction and introduction of the UMCG Research Data Catalogue.",
    },
  ],
});
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader id="page-header" title="Introduction" />
    </template>

    <template #side>
      <SideNavigation
        title="Introduction"
        :items="[
          { id: 'introduction', label: 'Introduction' },
          { id: 'request', label: 'Prepare your request' },
          { id: 'process', label: 'Request handling process' },
          { id: 'commercial', label: 'Commercial parties' },
        ]"
        header-target="#page-header"
      />
    </template>

    <template #main>
      <ContentBlocks>
        <!-- Introduction -->
        <ContentBlock id="introduction" title="Introduction">
          <p class="mb-4">
            The UMCG Research Data Catalogue is designed to help you easily find
            sample and data collections of interest based on various parameters,
            such as type of samples and/or data, age groups, and relevant
            keywords. You can also select standardized tools or questionnaires
            used to find corresponding collections.
          </p>

          <p class="mb-4">
            We would like to provide you with information regarding the use of
            the catalogue, considerations before submitting a request, the
            request procedure, and how to collaborate with us as a commercial
            partner.
          </p>

          <p class="mb-2 font-semibold">
            For each collection, the following information is available:
          </p>
          <ul class="list-disc ml-6 mb-4 [&_li]:mb-1.5">
            <li>Key researcher related to the collection</li>
            <li>Type and size of the population</li>
            <li>Available data and samples</li>
            <li>Subpopulations</li>
            <li>Collection events</li>
            <li>Publications, including a design paper when available</li>
            <li>Relevant access conditions applicable to the collection</li>
            <li>
              Additional documentation such as codebooks or informed consent
              forms
            </li>
          </ul>
        </ContentBlock>

        <ContentBlock id="request" title="Prepare your request">
          <p class="mb-4">
            When you come across a collection of interest, you can use the
            contact button to get in touch with the researcher managing the
            collection as a start for your request for samples and data. To help
            prepare your request, here are common questions and general
            conditions that apply for the use of UMCG samples and data.
          </p>

          <h3 class="mt-4 mb-2 font-semibold">Common questions</h3>
          <ul class="list-disc ml-6 mb-4 [&_li]:mb-1.5">
            <li>What is the purpose of the study?</li>
            <li>What are the exact number of samples and volumes needed?</li>
            <li>Which (clinical) data parameters are required?</li>
            <li>What are the expected outcomes of the study?</li>
            <li>Are there other partners involved in the research project?</li>
          </ul>

          <h3 class="mt-4 mb-2 font-semibold">General conditions</h3>
          <ul class="list-disc ml-6 mb-4 [&_li]:mb-1.5">
            <li>
              Any study performed with data and samples from a collection needs
              to fit with the overall purpose of the collection.
            </li>
            <li>
              Each individual collection may have their own more specified terms
              and conditions for accessing samples and data.
            </li>
            <li>
              A fee may apply for using the collection. Please enquire with the
              researcher of the collection about what fees apply.
            </li>
            <li>
              Volumes generally available per aliquot for requests per
              participant/patient:
              <table class="table-auto border mt-2">
                <thead>
                  <tr class="border">
                    <th class="border px-2 py-1 text-left">Type of sample</th>
                    <th class="border px-2 py-1 text-left">
                      Volume per aliquot
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr class="border">
                    <td class="border px-2 py-1">Serum</td>
                    <td class="border px-2 py-1">0.5–2.0 ml</td>
                  </tr>
                  <tr class="border">
                    <td class="border px-2 py-1">Plasma</td>
                    <td class="border px-2 py-1">0.5–2.0 ml</td>
                  </tr>
                  <tr class="border">
                    <td class="border px-2 py-1">Urine</td>
                    <td class="border px-2 py-1">2.0 ml</td>
                  </tr>
                </tbody>
              </table>
            </li>
          </ul>
        </ContentBlock>

        <ContentBlock id="process" title="Request handling process">
          <p class="mb-4">
            Requests for samples and/or data or initiation of a collaboration
            are handled by the researchers managing the particular collection.
            Contact details are available on each collection profile page.
          </p>

          <ol class="list-decimal ml-6 mb-4 [&_li]:mb-2">
            <li>
              After you have contacted the researcher of the collection, they
              will request more information about your enquiry or provide
              documentation to describe your request in more detail.
            </li>
            <li>
              If applicable, the request will be assessed by a local scientific
              review committee related to the collection.
            </li>
            <li>
              Each request will have to be assessed by the UMCG Central Ethical
              Review Committee.
              <ul class="list-disc ml-6 mt-1 [&_li]:mb-1">
                <li>
                  The researcher of the collection will submit the request to
                  the review committee.
                </li>
              </ul>
            </li>
            <li>
              If data and/or samples are transferred or shared with a third
              party, a contract will be prepared.
              <ul class="list-disc ml-6 mt-1 [&_li]:mb-1">
                <li>
                  The researcher of the collection will request the appropriate
                  contract templates from the UMCG legal department.
                </li>
              </ul>
            </li>
            <li>
              Samples and data will be shared by the means that have been
              approved of by both parties.
            </li>
          </ol>
        </ContentBlock>

        <ContentBlock id="commercial" title="Commercial parties">
          <p class="mb-4">
            As UMCG, we value research collaborations with commercial parties,
            because together we can create the impact on patients and healthcare
            that we all envision. Existing samples and data from our collections
            can be a valuable resource for these collaborations.
          </p>

          <p class="mb-2 font-semibold">
            Important elements of such a collaboration:
          </p>
          <ul class="list-disc ml-6 mb-4 [&_li]:mb-1.5">
            <li>Publication of research outcomes</li>
            <li>
              Return of generated data, for example through analyses performed
              on samples
            </li>
            <li>
              Samples and data will only be available within the purposes set
              forth in the project proposal
            </li>
            <li>
              Samples and data will not be shared for perpetual future use, but
              remain in stewardship of the UMCG
            </li>
          </ul>

          <p class="mb-4">
            Whether a collection is available for research collaborations with
            commercial parties depends on the individual setup of the
            collection. This information will either be available on the
            individual collection pages, or you can contact the researcher of
            the collection to enquire about the possibilities for a
            collaboration.
          </p>
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
