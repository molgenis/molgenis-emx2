<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader id="page-header" title="About">
        <template #prefix>
          <BreadCrumbs />
        </template>
      </PageHeader>
    </template>

    <template #side>
      <SideNavigation
        title="About"
        :items="[
          { id: 'intro', label: 'Introduction' },
          { id: 'use', label: 'Use' },
          { id: 'video', label: 'LongITools Video' },
          { id: 'molgenis', label: 'MOLGENIS Software' },
          { id: 'involvement', label: 'How to Get Involved' },
          { id: 'acknowledgements', label: 'Acknowledgements' },
        ]"
        header-target="#page-header"
      />
    </template>

    <template #main>
      <ContentBlocks>
        <ContentBlock id="intro" title="Introduction">
          Developed by the Genomics Coordination Center (GCC) at the University
          Medical Center Groningen (UMCG), the European Health Research Data and
          Sample Catalogue is a joint metadata catalogue initiative designed to
          bring together catalogues from various EU research projects and
          networks. It promotes data sharing and reuse, and supports
          advancements in public health. The catalogue contains only
          metadata—information about data, not the data itself.
        </ContentBlock>

        <ContentBlock id="use" title="Use">
          Institutions and data sources such as cohort studies—often involved in
          multi-centre consortia—add their metadata to the catalogue as part of
          consortium activities. They may also upload variables and mappings to
          the consortium's Common Data Model.

          <br /><br />

          Researchers can search within and across consortia to find harmonised
          variables for their own studies. Visitors can explore data
          collections, variables, consortia, networks, and more.

          <br /><br />

          Institutions not part of a consortium are also welcome to contribute.
          Please contact
          <a
            href="mailto:support@molgenis.org"
            class="text-blue-500 underline hover:bg-blue-50"
          >
            support@molgenis.org </a
          >.
        </ContentBlock>

        <ContentBlock id="video" title="LongITools Video">
          <a
            href="https://www.youtube.com/watch?v=PyRElhVzzb4&ab_channel=LongITools"
            target="_blank"
            rel="noopener noreferrer"
            class="text-blue-500 underline hover:bg-blue-50"
          >
            Watch the LongITools introduction video on YouTube
          </a>
        </ContentBlock>

        <ContentBlock id="molgenis" title="MOLGENIS Software">
          This database was built using the open-source
          <a
            href="https://github.com/molgenis/molgenis-emx2"
            target="_blank"
            rel="noopener noreferrer"
            class="text-blue-500 underline hover:bg-blue-50"
          >
            MOLGENIS molgenis-emx2
          </a>
          software (licensed under LGPLv3).

          <br /><br />

          If you use this resource, please cite:

          <ul class="list-disc pl-5 pt-2">
            <li>
              van der Velde, K. Joeri, et al. "MOLGENIS research: advanced
              bioinformatics data software for non-bioinformaticians."
              <em>Bioinformatics</em> 35.6 (2018): 1076.
            </li>
            <li>
              Swertz, Morris A., et al. "The MOLGENIS toolkit: rapid prototyping
              of biosoftware at the push of a button."
              <em>BMC Bioinformatics</em>. Vol. 11. BioMed Central, 2010.
            </li>
          </ul>

          <br />

          Learn more at
          <a
            href="https://www.molgenis.org"
            target="_blank"
            rel="noopener noreferrer"
            class="text-blue-500 underline hover:bg-blue-50"
          >
            www.molgenis.org
          </a>
          or visit our
          <a
            href="https://ticket.molgenis.net/help/en-gb"
            target="_blank"
            rel="noopener noreferrer"
            class="text-blue-500 underline hover:bg-blue-50"
          >
            knowledge base </a
          >. Full documentation:
          <a
            href="https://molgenis.github.io/molgenis-emx2/#/molgenis/use"
            target="_blank"
            rel="noopener noreferrer"
            class="text-blue-500 underline hover:bg-blue-50"
          >
            molgenis-emx2 docs </a
          >.
        </ContentBlock>

        <ContentBlock id="involvement" title="How to Get Involved">
          For any questions about MOLGENIS software or this catalogue, feel free
          to reach out to:
          <a
            href="mailto:support@molgenis.org"
            class="text-blue-500 underline hover:bg-blue-50"
          >
            support@molgenis.org </a
          >.
        </ContentBlock>

        <ContentBlock id="acknowledgements" title="Acknowledgements">
          This catalogue was created with the support of many partners. Special
          thanks to:
          <ul class="list-disc pl-5 pt-2">
            <li>European Child Cohort Network</li>
            <li>LifeCycle Consortium</li>
            <li>ATHLETE Consortium</li>
            <li>LongITools Consortium</li>
          </ul>
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>

<script setup lang="ts">
import { useHead } from "#app";

useHead({ title: "About" });
</script>
