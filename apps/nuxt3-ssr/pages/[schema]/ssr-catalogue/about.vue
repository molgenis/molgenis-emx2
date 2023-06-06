<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader title="About">
        <template #prefix>
          <BreadCrumbs :crumbs="[]" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        title="About"
        :items="[
          { id: 'intro', label: 'Introduction' },
          { id: 'standards', label: 'Standards' },
          { id: 'contact', label: 'Contact' },
          { id: 'privacy', label: 'Privacy' },
          { id: 'terms', label: 'Terms' },
          { id: 'other', label: 'Links' },
        ]" />
    </template>
    <template #main>
      <ContentBlocks>
        <ContentBlock id="intro" title="Introduction">
          This meta-data catalogue was commissioned by the University Medical
          Centre Groningen (UMCG). The aim of the UMCG Research Data Catalogue
          is to increase the visibility of UMCG data and sample resources and
          thereby stimulate their findability, accessibility and reuse, three
          major pillars of the
          <a
            href="https://www.go-fair.org/fair-principles/"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >FAIR principles</a
          >. The UMCG Research Data Catalogue includes a wide variety of data
          and sample resources, from large population-based cohorts and clinical
          biobanks to studies and everything in between. Some resources date
          back all the way to 1962. The catalogue was developed by the
          <a
            href="https://umcgresearch.org/w/gcc"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >Genomics Coordination Centre (GCC)</a
          >, the
          <a
            href="https://umcgresearch.org/w/umcg-cohort-and-biobank-coordination-hub"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >Cohort and Biobank Coordination Hub (CBCH)</a
          >
          and the Digital Competence Center (DCC) of the UMCG. This database was
          created using the MOLGENIS molgenis-emx2 open source software
          (license: LGPLv3). Please cite
          <a
            href="https://doi.org/10.1093/bioinformatics/bty742"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >Van der Velde et al (2018)</a
          >
          or
          <a
            href="https://doi.org/10.1186/1471-2105-11-S12-S12"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >Swertz et al (2010)</a
          >
          on use.
        </ContentBlock>
        <ContentBlock
          id="standards"
          title="National and international standards">
          Where possible, the meta-data in this catalogue was entered according
          to international standards, including
          <a
            href="https://icd.who.int/browse10/2019/en#/"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >ICD-10 (International Statistical Classification of Diseases and
            Related Health Problems)</a
          >,
          <a
            href="https://www.orphadata.com/orphanet-nomenclature-for-coding/"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >ORPHA codes (Orphanet nomenclature of rare diseases)</a
          >
          and
          <a
            href="https://github.com/BBMRI-ERIC/miabis"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >MIABIS (Minimum Information About BIobank data Sharing)</a
          >.
        </ContentBlock>
        <ContentBlock id="contact" title="Contact">
          If you are interested in one of the UMCG data and sample resources,
          please use the contact details mentioned on the individual resource’s
          profile page for correspondence. For general questions about the UMCG
          Research Data Catalogue please contact
          <a
            href="mailto:researchdatacatalogue@umcg.nl"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >researchdatacatalogue@umcg.nl</a
          >
          . Questions about the Molgenis software can be sent to
          <a
            href="mailto:molgenis-support@umcg.nl"
            rel="noopener noreferrer"
            target="_blank"
            class="text-blue-500 underline hover:bg-blue-50"
            >molgenis-support@umcg.nl</a
          >.
        </ContentBlock>
        <ContentBlock id="privacy" title="Disclaimer &amp; Privacy">
          The metadata has been filled in manually by the steward of each
          resource or automatically from linked sources. No rights can be
          derived from it. The UMCG is not responsible for any errors in the
          metadata or the content of the resource profiles on
          umcgresearchdatacatalogue.nl. Data from resource participants cannot
          be directly accessed in or through this meta-data catalogue. Access to
          the resources is restricted and use must fit within the informed
          consent that participants signed before collection. Researchers can
          request access to data and samples. Please see the individual resource
          pages for details.
        </ContentBlock>
        <ContentBlock id="terms" title="Terms and Conditions">
          This website and its content are property of the UMCG and cooperating
          centres ©2023. All rights reserved. Redistribution or reproduction of
          (part of) the content in any form is prohibited other than the
          following:
          <List>
            <ListItem>
              You may print or download to a local hard disk part of the content
              for your personal and non-commercial use only.
            </ListItem>
            <ListItem>
              You may use part of the content in grant applications or
              publications, but only if you acknowledge the website as the
              source of the material used.
            </ListItem>
            <ListItem>
              You may copy and transfer part of the content to individual third
              parties for their personal use, but only if you acknowledge the
              website as the source of the material transferred.
            </ListItem>
            <ListItem>
              You may not, except with our express written permission, transmit
              or store any of the content in another website or other form of
              electronic retrieval system.
            </ListItem>
            <ListItem>
              You may not distribute or commercially exploit the UMCG Research
              Data Catalogue website content.
            </ListItem>
          </List>
        </ContentBlock>
        <ContentBlock id="other" title="Other resource catalogues">
          <List>
            <ListItem>
              <a
                href="https://catalogue.bbmri.nl/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >Bbmri-NL</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://directory.bbmri-eric.eu/#/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >BBMRI-ERIC</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://www.maelstrom-research.org/page/catalogue"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >Maelstrom catalogue</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://www.healthinformationportal.eu/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >European Health Information Portal</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://dans.knaw.nl/nl/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >DANS</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://explore.openaire.eu/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >OpenAIRE</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://www.cessda.eu/Tools/Data-Catalogue"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >CESSDA</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://lifecycle-project.eu/for-scientists/variable-catalogue/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >LifeCycle</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://www.clinicaltrialsregister.eu/ctr-search/search"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >Clinical trial register EU</a
              >
            </ListItem>
            <ListItem>
              <a
                href="https://datasetsearch.research.google.com/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >Google Dataset Search</a
              >
            </ListItem>
            <ListItem>
              <a
                href="http://catalogue.dcvalliance.nl/"
                rel="noopener noreferrer"
                target="_blank"
                class="text-blue-500 underline hover:bg-blue-50"
                >DCV Alliance</a
              >
            </ListItem>
          </List>
          <p class="clear-both pt-5">
            <em>
              We regularly check the reliability of the information on the sites
              we link to. Nevertheless, the UMCG cannot be held accountable for
              the content of other websites, nor for the privacy protection or
              services provided by other websites.</em
            >
          </p>
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>

<script setup>
useHead({ title: "About" });
</script>
