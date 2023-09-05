<template>
  <Page>
    <PageHeader
      title="ERN CRANIO"
      :subtitle="`${center} Dashboard`"
      imageSrc="example-provider-header.jpg"
      height="large"
    />
    <div class="sidebar-layout">
      <aside class="sidebar">
        <h2>Dashboards</h2>
        <nav>
          <ul class="navlinks">
            <li>
              <router-link :to="{ name: 'provider-home' }">
                {{ center }} overview
              </router-link>
            </li>
            <li>
              <router-link :to="{ name: 'provider-cranio' }">
                Craniosynostosis
              </router-link>
            </li>
            <li>
              <router-link :to="{ name: 'provider-clp' }">
                Cleft lip and palate
              </router-link>
            </li>
            <li>
              <a href="#">
                Genetic deafness
              </a>
            </li>
            <li>
              <a href="#">
                Larynxcleft
              </a>
            </li>
          </ul>
        </nav>
        <ButtonAction id="btnSubmitPatient">
          <span>Submit patient</span>
          <PlusCircleIcon />
        </ButtonAction>
      </aside>
      <div class="main">
        <router-view></router-view>
      </div>
    </div>
  </Page>
</template>

<script setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { Page, PageHeader } from "molgenis-viz";
import { ButtonAction } from "molgenis-components";

import { PlusCircleIcon } from "@heroicons/vue/24/outline"

const route = useRoute();
const center = ref(route.params.provider);

</script>

<style lang="scss">
.sidebar-layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 2em;
  background-color: $gray-050;
  box-sizing: border-box;
  padding: 3em;
  justify-content: flex-start;
  align-items: flex-start;
  
  @media (min-width: 1524px) {
    max-width: 60vw;
  }
  
  .sidebar {
    background-color: $gray-000;
    box-sizing: content-box;
    padding: 2em;
    box-shadow: $box-shadow;
    border-radius: 8px;
    
    nav {
      padding: 0;
      margin: 2em 0;

      .navlinks {
        list-style: none;
        padding: 0;
        width: 100%;
        
        li {
          border-left: 5px solid transparent;
          margin-bottom: 1em;
          padding: 0.4em;
          padding-left: 1.5em;
          
          a {
            color: currentColor;
            
            &:hover, &:focus {
              color: var(--primary);
            }
          }
          
          &.link-selected {
            border-left-color: var(--primary);
            a {
              color: var(--primary);
            }
          }
        }
        
      }
    }
  }
  
  .main {
    .dashboard-content {
      display: grid;
      gap: 2em;
      
      .dashboard-section {
        background-color: $gray-000;
        box-shadow: $box-shadow;
        border-radius: 8px;
        box-sizing: content-box;
        padding: 1.65em;
        
        .chart-title {
          font-size: 16pt;          
        }
        
        p {
          margin-bottom: 0;
        }
      }
    }
  }
}

#btnSubmitPatient {
  width: 100%;
  padding: 0.7em;
  
  svg {
    width: 21px;
    margin-left: 4px;
    margin-top: -2px;
    path {
      stroke-width: 2;
    }
  }
}
</style>