<template>
  <div class="breadcrumbs-container">
    <nav class="breadcrumbs">
      <ul>
        <li v-for="crumb in breadcrumbs">
          <router-link :to="{ name: crumb.name }" v-if="crumb.name === 'home'">
            <HomeIcon class="heroicons heroicons-outline heroicons-home" />
          </router-link>
          <router-link :to="{ name: crumb.name }" v-else>{{
            crumb.label
          }}</router-link>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { HomeIcon } from "@heroicons/vue/24/outline";

const breadcrumbs = ref([{ name: "home" }]);

function getBreadCrumbs() {
  const route = useRoute();
  route.meta.breadcrumbs.map((breadcrumb) =>
    breadcrumbs.value.push(breadcrumb)
  );
}

onMounted(() => getBreadCrumbs());
</script>

<style lang="scss">
.breadcrumbs-container {
  box-sizing: content-box;
  padding: 1em;

  .breadcrumbs {
    ul {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 1em;

      li {
        position: relative;

        .heroicons {
          $size: 21px;
          width: $size;
          height: $size;
          margin-top: -3px;
        }

        &::after {
          content: "/";
          margin-left: 1em;
        }

        &:last-child {
          &::after {
            content: none;
          }
        }
      }
    }
  }
}
</style>
