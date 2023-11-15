<script setup>
const route = useRoute();
const config = useRuntimeConfig();

const cat = route.params.catalogue;
const query = `{
        Networks(filter:{id:{equals:"${cat}"}}) {
              id,
              dataSources_agg{count}
              cohorts_agg{count}
              logo{url}
       }
       Variables_agg(filter:{resource:{id:{equals:"${cat}"}}}) {
          count
        }
    }`;
const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query },
  }
);
const catalogue = data.value.data?.Networks[0];

const menu = [
  {
    label: `${catalogue.id}`,
    link: `/${route.params.schema}/ssr-catalogue/${catalogue.id}`,
  },
];
if (catalogue.cohorts_agg?.count > 0)
  menu.push({
    label: "Cohorts",
    link: `/${route.params.schema}/ssr-catalogue/${catalogue.id}/cohorts`,
  });
if (catalogue.dataSources_agg?.count > 0)
  menu.push({
    label: "Data sources",
    link: `/${route.params.schema}/ssr-catalogue/${catalogue.id}/datasources`,
  });
if (data.value.data.Variables_agg?.count > 0)
  menu.push({
    label: "Variables",
    link: `/${route.params.schema}/ssr-catalogue/${catalogue.id}/variables`,
  });
menu.push({
  label: "Variables",
  link: `/${route.params.schema}/ssr-catalogue/${catalogue.id}/variables`,
});
// todoswqki
// menu.push({
//   label: "About",
//   link: `/${route.params.schema}/ssr-catalogue/${catalogue.id}/about`,
// }),
  menu.push({
    label: "Other catalogues",
    link: `/${route.params.schema}/ssr-catalogue`,
  });

// { label: "Databanks", link: `/${schema}/ssr-catalogue/databanks` },
// config.public.cohortOnly
//     ? undefined
//     : ,
// // { label: "Statistical Methods", link: "#" },
// // { label: "Tables", link: "#" },
// // { label: "Manuals", link: "#" },
// { label: "About", link: `/${schema}/ssr-catalogue/about` },
</script>

<template>
  <header class="antialiased px-5 lg:px-0 xl:bg-white">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo
          :link="`/${route.params.schema}/ssr-catalogue/${catalogue.id}`"
          :image="catalogue.logo?.url"
        />
        <MainNavigation :navigation="menu" :invert="true" />
        <!--  <div class="w-[450px]">
           <SearchBar />
        </div>-->

        <!-- <HeaderButton label="Favorites" icon="star" />
        <HeaderButton label="Account" icon="user" /> -->
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <!-- <HamburgerMenu :navigation="menu" /> -->

          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile
              :link="`/${route.params.schema}/ssr-catalogue/${catalogue.id}`"
              :image="catalogue.logo?.url"
            />
          </div>

          <div class="flex gap-3">
            <!-- <HeaderButton label="Favorites" icon="star" />
            <HeaderButton label="Account" icon="user" /> -->
          </div>
        </div>

        <MainNavigation :navigation="menu" :showMoreButton="false" />
        <div class="w-full pt-6">
          <!-- <SearchBar /> -->
        </div>
      </div>
    </Container>
  </header>
</template>
