<script setup lang="ts">
const config = useRuntimeConfig()
const route = useRoute()

const query = `query Cohorts ($pid: String){
    Cohorts(filter: { pid: { equals: [$pid] } }){
        name
        description
    }}`;
const variables = { pid: route.params.cohort };

const { data, pending, error, refresh } = await useFetch(
    "/catalogue/catalogue/graphql",
    {
        baseURL: config.public.apiBase,
        method: "POST",
        body: { query, variables }
    }
);

</script>
<template>
    <LayoutsDetailPage>
        <template #header>
            <PageHeader :title="data.data.Cohorts[0]?.name"
                description="Short description of the cohort could be placed here.">
                <template #prefix>
                    <BreadCrumbs />
                </template>
                <template #title-suffix>
                    <IconButton icon="star" label="Favorite" />
                </template>
            </PageHeader>
        </template>
        <template #side>
            <SideNavigation />
        </template>
        <template #main>
            <ContentBlocks>
                <ContentBlockIntro />
                <ContentBlockDescription title="Description"
                    description="Lifelines NEXT is a prospective birth cohort aiming to include 1.500 pregnant women and their children residing in the northern provinces of The Netherlands. The women are followed from the third month of their pregnancy with the aim of investigating in pregnants/mothers and their child the effect of early life or pre-conceptional transgenerational events on healthy ageing and chronic disease in (early) childhood. As of xxxx, partners were also invited to enroll in Lifelines NEXT. Standardized protocols and guidelines are available upon request.<br><br><strong>Keywords</strong><br>Children of the 90s, birth,  Genetics, DNA, RNA, Genetic disease, heritable disease " />
                <ContentBlockGeneralDesign title="General Design" />
                <ContentBlockAttachedFiles title="Attached Files Generic Example" />
                <ContentBlockContact title="Contact and Contributers" />
                <ContentBlockVariables title="Variables & Topics"
                    description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga." />
                <ContentBlockData title="Available Data & Samples"
                    description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga." />
                <ContentBlockSubpopulations title="Subpopulations"
                    description="Explanation about subpopulations and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga." />
                <ContentBlockCollectionEvents title="Collection Events"
                    description="Explanation about collection events and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga." />
                <ContentBlockNetwork title="Networks"
                    description="Networks Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga." />
                <ContentBlockPartners title="Partners"
                    description="Partners Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga." />
            </ContentBlocks>
        </template>
    </LayoutsDetailPage>
</template>

