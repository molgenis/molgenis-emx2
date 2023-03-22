<template>
  <LayoutsDetailPage>
    <template #header>
      <!-- <PageHeader
        :title="collectionEvent?.name"
        :description="collectionEvent?.description"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader> -->
    </template>
    <template #side>
      <!-- <SideNavigation :title="collectionEvent?.name" :items="tocItems" /> -->
    </template>
    <template #main>
      <p>
        <h2 class="mb-5 uppercase text-heading-4xl font-display">
          >About</h2
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >This meta-data catalogue was commissioned by the University Medical
          Centre Groningen (UMCG). The aim of the UMCG Research Data Catalogue
          is to increase the visibility of UMCG data and sample resources and
          thereby stimulate their findability, accessibility and reuse, three
          major pillars of the </span
        ><a
          href="https://www.go-fair.org/fair-principles/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >FAIR principles</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >.&nbsp;</span
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >The UMCG Research Data Catalogue includes a wide variety of data and
          sample resources, from large population-based cohorts and clinical
          biobanks to studies and everything in between. Some resources date
          back all the way to 1962.&nbsp;</span
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >The catalogue was developed by the </span
        ><a
          href="https://umcgresearch.org/w/gcc"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Genomics Coordination Centre (GCC)</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >, the </span
        ><a
          href="https://umcgresearch.org/w/umcg-cohort-and-biobank-coordination-hub"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Cohort and Biobank Coordination Hub (CBCH)</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >&nbsp; and the Digital Competence Center (DCC) of the UMCG. This
          database was created using the MOLGENIS molgenis-emx2 open source
          software (license: LGPLv3). Please cite </span
        ><a
          href="https://doi.org/10.1093/bioinformatics/bty742"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Van der Velde et al (2018)</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)">
          or </span
        ><a
          href="https://doi.org/10.1186/1471-2105-11-S12-S12"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Swertz et al (2010)</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)">
          on use.</span
        >
      </p>
      <p>
        <strong style="background-color: transparent; color: rgb(0, 0, 0)"
          >National and international standards</strong
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >Where possible, the meta-data in this catalogue was entered according
          to international standards, including </span
        ><a
          href="https://icd.who.int/browse10/2019/en#/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >ICD-10 (International Statistical Classification of Diseases and
          Related Health Problems)</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >, </span
        ><a
          href="https://www.orphadata.com/orphanet-nomenclature-for-coding/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >ORPHA codes (Orphanet nomenclature of rare diseases)</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)">
          and </span
        ><a
          href="https://github.com/BBMRI-ERIC/miabis"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >MIABIS (Minimum Information About BIobank data Sharing)</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >.</span
        >
      </p>
      <p><br /></p>
      <p>
        <strong style="background-color: transparent; color: rgb(0, 0, 0)"
          >Contact</strong
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >If you are interested in one of the UMCG data and sample resources,
          please use the contact details mentioned on the individual resource’s
          profile page for correspondence. For general questions about the UMCG
          Research Data Catalogue please contact </span
        ><a
          href="mailto:researchdatacatalogue@umcg.nl"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >researchdatacatalogue@umcg.nl</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)">
          .&nbsp; Questions about the Molgenis software can be sent to </span
        ><a
          href="mailto:molgenis-support@umcg.nl"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >molgenis-support@umcg.nl</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >.</span
        >
      </p>
      <p><br /></p>
      <p>
        <strong style="background-color: transparent; color: rgb(0, 0, 0)"
          >Disclaimer &amp; Privacy</strong
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >The metadata has been filled in manually by the steward of each
          resource or automatically from linked sources. No rights can be
          derived from it. The UMCG is not responsible for any errors in the
          metadata or the content of the resource profiles on
          umcgresearchdatacatalogue.nl.&nbsp;</span
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >Data from resource participants cannot be directly accessed in or
          through this meta-data catalogue. Access to the resources is
          restricted and use must fit within the informed consent that
          participants signed before collection. Researchers can request access
          to data and samples. Please see the individual resource pages for
          details.&nbsp;</span
        >
      </p>
      <p><br /></p>
      <p>
        <strong style="background-color: transparent; color: rgb(0, 0, 0)"
          >Terms and Conditions</strong
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >&nbsp;</span
        >
      </p>
      <p>
        <span style="background-color: transparent; color: rgb(0, 0, 0)"
          >This website and its content are property of the UMCG and cooperating
          centres ©2023. All rights reserved. Redistribution or reproduction of
          (part of) the content in any form is prohibited other than the
          following:</span
        >
      </p>
      <ul>
        <li>
          <span style="background-color: transparent"
            >You may print or download to a local hard disk part of the content
            for your personal and non-commercial use only.</span
          >
        </li>
        <li>
          <span style="background-color: transparent"
            >You may use part of the content in grant applications or
            publications, but only if you acknowledge the website as the source
            of the material used.</span
          >
        </li>
        <li>
          <span style="background-color: transparent"
            >You may copy and transfer part of the content to individual third
            parties for their personal use, but only if you acknowledge the
            website as the source of the material transferred.</span
          >
        </li>
        <li>
          <span style="background-color: transparent"
            >You may not, except with our express written permission, transmit
            or store any of the content in another website or other form of
            electronic retrieval system.</span
          >
        </li>
        <li>
          <span style="background-color: transparent"
            >You may not distribute or commercially exploit the UMCG Research
            Data Catalogue website content.</span
          >
        </li>
      </ul>
      <p><br /></p>
      <p>
        <strong style="background-color: transparent; color: rgb(0, 0, 0)"
          >Other resource catalogues</strong
        >
      </p>
      <p>
        <a
          href="https://catalogue.bbmri.nl/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Bbmri-NL</a
        >
      </p>
      <p>
        <a
          href="https://directory.bbmri-eric.eu/#/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >BBMRI-ERIC</a
        >
      </p>
      <p>
        <a
          href="https://www.maelstrom-research.org/page/catalogue"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Maelstrom catalogue</a
        >
      </p>
      <p>
        <a
          href="https://www.healthinformationportal.eu/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >European Health Information Portal</a
        >
      </p>
      <p>
        <a
          href="https://dans.knaw.nl/nl/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >DANS</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >&nbsp;</span
        >
      </p>
      <p>
        <a
          href="https://explore.openaire.eu/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >OpenAIRE</a
        >
      </p>
      <p>
        <a
          href="https://www.cessda.eu/Tools/Data-Catalogue"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >CESSDA</a
        >
      </p>
      <p>
        <a
          href="https://lifecycle-project.eu/for-scientists/variable-catalogue/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >LifeCycle</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >&nbsp;</span
        >
      </p>
      <p>
        <a
          href="https://www.clinicaltrialsregister.eu/ctr-search/search"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Clinical trial register EU</a
        ><span style="background-color: transparent; color: rgb(0, 0, 0)"
          >&nbsp;</span
        >
      </p>
      <p>
        <a
          href="https://datasetsearch.research.google.com/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >Google Dataset Search</a
        >
      </p>
      <p>
        <a
          href="http://catalogue.dcvalliance.nl/"
          rel="noopener noreferrer"
          target="_blank"
          style="background-color: transparent; color: rgb(5, 99, 193)"
          >DCV Alliance</a
        >
      </p>
      <p>
        <em style="background-color: transparent; color: rgb(0, 0, 0)"
          >We regularly check the reliability of the information on the sites we
          link to. Nevertheless, the UMCG cannot be held accountable for the
          content of other websites, nor for the privacy protection or services
          provided by other websites.​​​​​​​</em
        >
      </p>
      <p><br /></p>
      <p><br /></p>
      <p><br /></p>
      <p><br /></p>
    </template>
  </LayoutsDetailPage>
</template>
