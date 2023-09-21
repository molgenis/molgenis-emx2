<script setup>
import AppWrapper from "../AppWrapper.vue";
import AppHeader from "../AppHeader.vue";
import DetailPage from "../layouts/DetailPage.vue";
import SideNavigation from "../SideNavigation.vue";
import ContentBlocks from "../ContentBlocks.vue";
import ContentBlockIntro from "../ContentBlockIntro.vue";
import ContentBlockDescription from "../ContentBlockDescription.vue";
import ContentBlockGeneralDesign from "../ContentBlockGeneralDesign.vue";
import ContentBlockAttachedFiles from "../ContentBlockAttachedFiles.vue";
import ContentBlockContact from "../ContentBlockContact.vue";
import ContentBlockVariables from "../ContentBlockVariables.vue";
import ContentBlockData from "../ContentBlockData.vue";
import ContentBlockSubpopulations from "../ContentBlockSubpopulations.vue";
import ContentBlockCollectionEvents from "../ContentBlockCollectionEvents.vue";
import ContentBlockNetwork from "../ContentBlockNetwork.vue";
import ContentBlockPartners from "../ContentBlockPartners.vue";
import BreadCrumbs from "../BreadCrumbs.vue";
import PageHeader from "../PageHeader.vue";
import IconButton from "../IconButton.vue";
</script>

<template>
  <AppWrapper>
    <template #header>
      <AppHeader />
    </template>

    <DetailPage>
      <template #header>
        <PageHeader
          title="Lifelines Next"
          description="Short description of the cohort could be placed here."
        >
          <template #prefix>
            <BreadCrumbs :crumbs="{ Home: '/', Cohorts: '/cohorts' }" />
          </template>
          <template #title-suffix>
            <!-- <IconButton icon="star" label="Favorite" /> -->
          </template>
        </PageHeader>
      </template>
      <template #side>
        <SideNavigation />
      </template>
      <template #main>
        <ContentBlocks>
          <ContentBlockIntro />
          <ContentBlockDescription
            title="Description"
            description="Lifelines NEXT is a prospective birth cohort aiming to include 1.500 pregnant women and their children residing in the northern provinces of The Netherlands. The women are followed from the third month of their pregnancy with the aim of investigating in pregnants/mothers and their child the effect of early life or pre-conceptional transgenerational events on healthy ageing and chronic disease in (early) childhood. As of xxxx, partners were also invited to enroll in Lifelines NEXT. Standardized protocols and guidelines are available upon request.<br><br><strong>Keywords</strong><br>Children of the 90s, birth,  Genetics, DNA, RNA, Genetic disease, heritable disease "
          />
          <ContentBlockGeneralDesign title="General Design" />
          <ContentBlockAttachedFiles title="Attached Files Generic Example" />
          <ContentBlockContact title="Contact and Contributors" />
          <ContentBlockVariables
            title="Variables &amp; Topics"
            description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
          />
          <ContentBlockData
            title="Available Data &amp; Samples"
            description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
          />
          <ContentBlockSubpopulations
            title="Subpopulations"
            description="Explanation about subpopulations and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
          />
          <ContentBlockCollectionEvents
            title="Collection Events"
            description="Explanation about collection events and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
          />
          <ContentBlockNetwork
            title="Networks"
            description="Networks Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
          />
          <ContentBlockPartners
            title="Partners"
            description="Partners Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
          />
        </ContentBlocks>
      </template>
    </DetailPage>
  </AppWrapper>
</template>
