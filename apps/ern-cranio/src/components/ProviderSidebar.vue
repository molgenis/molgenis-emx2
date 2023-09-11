<template>
  <aside class="sidebar">
    <h2>Dashboards</h2>
    <nav>
      <ul class="navlinks">
        <li>
          <router-link :to="{ name: 'provider-overview', params: { id: organization.id } }">
            Home
          </router-link>
        </li>
        <li>
          <Accordion id="cranio-nav" title="Craniosynostosis">
            <p>All Centers</p>
            <ul>
              <li>
                <router-link :to="{ name: 'provider-cs-all-general', params: { id: organization.id } }">
                  General overview
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-cs-all-surgical', params: { id: organization.id }}">
                  Surgical Overview
                </router-link>
              </li>
            </ul>
            <p>Your Center</p>
            <ul>
              <li>
                <router-link :to="{ name: 'provider-cs-center-overview', params: { id: organization.id }}">
                  General overview
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-cs-center-surgical', params: { id: organization.id }}">
                  Surgical overview
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
        <li>
          <Accordion id="clp-nav" title="Cleft lip and palate">
            <ul>
              <li>
                <router-link :to="{name: 'provider-clp-your-center', params: { id: organization.id } }">
                  Your center's overview
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-clp-all-centers', params: { id: organization.id} }">
                  All centers
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
        <li>
          <router-link :to="{ name: 'provider-genetic-deafness', params: { id: organization.id } }">
            Genetic Deafness
          </router-link>
        </li>
        <li>
          <router-link :to="{ name: 'provider-larnyxcleft', params: { id: organization.id } }">
            Larynxcleft
          </router-link>
        </li>
      </ul>
    </nav>
    <ButtonAction id="btnSubmitPatient">
      <span>Submit patient</span>
      <PlusCircleIcon />
    </ButtonAction>
  </aside>
</template>

<script setup>
import { Accordion } from "molgenis-viz";
import { ButtonAction } from "molgenis-components";
import { PlusCircleIcon } from "@heroicons/vue/24/outline";

const props = defineProps({
  user: String,
  organization: Object,
});

</script>

<style lang="scss">
.sidebar {
  background-color: $gray-000;
  box-sizing: content-box;
  padding: 2em 1.5em;
  box-shadow: $box-shadow;
  border-radius: 8px;

  nav {
    padding: 0;
    margin-top: 1em;
    margin-bottom: 2.5em;

    ul,
    ul li ul {
      list-style: none;
      padding: 0;
      margin: 0;
      width: 100%;
    }
    
    .accordion {
      margin: 0;
      border: none;
      
      .accordion-heading {
        padding: 0;
        background-color: transparent;
        
        .toggle-label {
          font-size: 14pt;
        }
      }
      .accordion-content { 
        padding: 0;
      }
    }
    
    li {
      margin: 0.6em 0;
      border-left: 3px solid transparent;
      padding-left: 1em;
      
      p {
        @include textTransform(bold);
        margin-top: 24px;
        padding-left: 1.5em;
        line-height: 1;
        color: $gray-600;
      }
      
      a {
        color: currentColor;

        &:hover,
        &:focus {
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

#btnSubmitPatient {
  width: 100%;
  padding: 0.6em;
  @include textTransform;
  color: $gray-000;

  svg {
    position: relative;
    width: 21px;
    margin-left: 4px;
    margin-top: -4px;
    path {
      stroke-width: 2;
    }
  }
}
</style>
