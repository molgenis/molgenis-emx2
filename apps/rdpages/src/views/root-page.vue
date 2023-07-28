<template>
  <Page>
    <PageHeader
      title="Rare Disease Applications"
      subtitle="Sitemap"
      imageSrc="sitemap-header.jpg"
      titlePositionX="center"
    />
    <PageSection aria-labelledby="sitemap-title" :verticalPadding="2">
      <h2 id="sitemap-title">Sitemap</h2>
      <ul>
        <li v-for="route in sitemap" :key="route.name">
          <p>
            {{ route.name }}:
            <router-link :to="route.path" v-if="route.name==='root'">
              {{ route.path }}
            </router-link>
          </p>
          <ul v-if="route.children.length" v-for="child in route.children">
            <li>
              <p>
                {{ child.name.replace(route.name,'').replace('-','') }}:
                <router-link :to="route.path + '/' + child.path">
                  {{ route.path + "/" + child.path }}
                </router-link>
              </p>
            </li>
          </ul>
        </li>
      </ul>
    </PageSection>
  </Page>
</template>

<script setup>
import { useRouter } from "vue-router";
import { Page, PageHeader, PageSection } from "molgenis-viz";
const router = useRouter();

const routes = router.getRoutes();
const sitemap = routes.filter((route) => {
  if (route.children.length || route.name === "root") {
    return route;
  }
});
</script>
