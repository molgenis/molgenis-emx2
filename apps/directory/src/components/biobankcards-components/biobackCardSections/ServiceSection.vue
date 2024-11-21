<script setup lang="ts">
import { IServices } from "../../../interfaces/directory";

withDefaults(
  defineProps<{
    services?: IServices[];
  }>(),
  {
    services: () => [],
  }
);
</script>

<template>
  <div>
    {{ services.length }} services available
    <div v-for="service in services">
      <div class="pl-2 pt-2 d-flex">
        <router-link
          :to="'/service/' + service.id"
          title="Service details"
          class="text-dark"
        >
          <span class="collection-name">
            {{ service.name }}
          </span>
        </router-link>
        <div class="ml-auto">
          <!-- {{ service }} -->
          <!-- <collection-selector
                      class="ml-auto"
                      :biobankData="biobank"
                      :collectionData="collectionDetail"
                      iconOnly
                      bookmark
                    /> -->
        </div>
      </div>
      <div>
        <small>
          <div class="p-1">
            <table class="layout-table w-100">
              <tr>
                <th scope="row" class="pr-1 align-top text-nowrap">Type:</th>
                <td>
                  <span>{{
                    service.serviceTypes.map((type) => type.label).join(", ")
                  }}</span>
                </td>
              </tr>
            </table>
          </div>
          <RouterLink
            :to="'/service/' + service.id"
            title="Service details"
            class="text-info ml-1 pl-1"
            >More details
          </RouterLink>
        </small>
      </div>
    </div>
  </div>
</template>
