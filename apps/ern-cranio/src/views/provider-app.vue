<template>
  <Page>
    <PageHeader
      title="ERN CRANIO Registry"
      :subtitle="`${organization.label}`"
      imageSrc="example-provider-header.jpg"
      height="large"
    />
    <Dashboard class="provider-dashboard-container">
      <ProviderSidebar :user="user" :organization="organization" />
      <router-view :user="user" :organization="organization"></router-view>
    </Dashboard>
  </Page>
</template>

<script setup>
import { Page, PageHeader, Dashboard } from "molgenis-viz";
import ProviderSidebar from "../components/ProviderSidebar.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

</script>

<style lang="scss">
.provider-dashboard-container {
  .dashboard-content {
    display: grid;
    grid-template-columns: 350px 1fr;
    align-items: flex-start;
    justify-content: flex-start;
    
    .provider-visualizations {
      display: grid;
      gap: 2em; 
    }

    .dashboard-box {
      box-sizing: content-box;
      padding: 1.3em;
      
      &.value-highlight {
        h2 {
          font-weight: 400;
          text-transform: none;
          letter-spacing: normal;
          font-size: 15pt;

          svg {
            width: 19px;
            margin-top: -2px;
            margin-right: 8px;
          }
        }
        p {
          margin: 0;
        }
      }
      
      h3,
      .d3-viz .chart-title,
      .d3-progress-meter .chart-title span {
        font-size: 14pt;
        font-weight: 400;
      }
      
      p {
        margin-bottom: 0;
      }
      
      .d3-progress-meter {
        .chart-title {
          margin: 0;
          span {
            font-size: 14pt;
          }
        }

        .chart {
          margin: 0;
        }
      }
    }
  }
}
</style>
