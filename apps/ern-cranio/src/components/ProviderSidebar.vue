<template>
  <aside class="sidebar">
    <h2>Dashboards</h2>
    <nav>
      <ul class="navlinks">
        <li>
          <router-link
            :to="{ name: 'provider-overview', params: { id: orgId } }"
          >
            Home
          </router-link>
        </li>
        <li>
          <Accordion id="cranio-nav" title="Craniosynostosis">
            <p>All Centers</p>
            <ul>
              <li>
                <router-link
                  :to="{
                    name: 'provider-cs-all-general',
                    params: { id: orgId },
                  }"
                >
                  General overview
                </router-link>
              </li>
              <li>
                <router-link
                  :to="{
                    name: 'provider-cs-all-surgical',
                    params: { id: orgId },
                  }"
                >
                  Surgical Overview
                </router-link>
              </li>
            </ul>
            <p>Your Center</p>
            <ul>
              <li>
                <router-link
                  :to="{
                    name: 'provider-cs-center-overview',
                    params: { id: orgId },
                  }"
                >
                  General overview
                </router-link>
              </li>
              <li>
                <router-link
                  :to="{
                    name: 'provider-cs-center-surgical',
                    params: { id: orgId },
                  }"
                >
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
                <router-link
                  :to="{
                    name: 'provider-clp-your-center',
                    params: { id: orgId },
                  }"
                >
                  Your center's overview
                </router-link>
              </li>
              <li>
                <router-link
                  :to="{
                    name: 'provider-clp-all-centers',
                    params: { id: orgId },
                  }"
                >
                  All centers
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
        <li>
          <router-link
            :to="{ name: 'provider-genetic-deafness', params: { id: orgId } }"
          >
            Genetic Deafness
          </router-link>
        </li>
        <li>
          <router-link
            :to="{ name: 'provider-larnyxcleft', params: { id: orgId } }"
          >
            Larynxcleft
          </router-link>
        </li>
      </ul>
    </nav>
    <p id="btnSubmitPatient">
      <span>Submit patient</span>
    </p>
  </aside>
</template>

<script setup>
import { Accordion } from "molgenis-viz";
import { ButtonAction } from "molgenis-components";
import { PlusCircleIcon } from "@heroicons/vue/24/outline";

import viewProps from "../data/props";
const props = defineProps(viewProps);
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

    $link-font-size: 12pt;

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
          font-size: $link-font-size;
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

      a,
      p {
        font-size: $link-font-size;
        margin: 0;
        line-height: 1;
      }

      p {
        @include textTransform;
        margin-top: 24px;
        padding-left: 1.5em;
        color: $gray-400;
      }

      a {
        color: currentColor;

        &:hover,
        &:focus {
          color: $ern-cranio-primary;
        }
      }

      &.link-selected {
        border-left-color: $ern-cranio-primary;
        a {
          color: $ern-cranio-primary;
        }
      }
    }
  }
}

#btnSubmitPatient {
  @include buttonLink;
  @include textTransform;

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
