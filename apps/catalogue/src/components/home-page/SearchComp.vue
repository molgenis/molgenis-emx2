<template>
  <div>
    <div class="row justify-content-md-center">
      <div class="col-sm-12 col-md-8">
        <main>
          <search-resource :resourceType="resourceType" />
        </main>
      </div>
    </div>
  </div>
</template>

<script>
import SearchResource from "@/components/SearchResource";

export default {
  name: "SearchComp",
  components: { SearchResource },
  props: {
    resourceType: {
      type: String, // one of Resource
      default: () => "institutions",
    },
  },
};
</script>

<style></style>
