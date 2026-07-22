<script setup lang="ts">
// @ts-ignore
import { Accordion } from "molgenis-viz";
import { ArrowUpTrayIcon } from "@heroicons/vue/24/outline";
</script>

<template>
  <aside class="sidebar">
    <h2>Dashboards</h2>
    <nav>
      <ul class="navlinks">
        <li>
          <router-link :to="{ name: 'home' }"> Your center </router-link>
        </li>
        <li>
          <Accordion
            id="cranio-nav"
            title="Craniosynostosis"
            :isOpenByDefault="true"
          >
            <p>All Centers</p>
            <ul>
              <li>
                <router-link :to="{ name: 'provider-cs-all-general' }">
                  General overview
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-cs-all-surgical' }">
                  Surgical Overview
                </router-link>
              </li>
            </ul>
            <p>Your Center</p>
            <ul>
              <li>
                <router-link :to="{ name: 'provider-cs-center-overview' }">
                  General overview
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-cs-center-surgical' }">
                  Surgical overview
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
        <li>
          <Accordion
            id="clp-nav"
            title="Cleft lip and palate"
            :isOpenByDefault="true"
          >
            <ul>
              <li>
                <router-link :to="{ name: 'provider-clp-your-center' }">
                  Your center's overview
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-clp-level-1' }">
                  Level 1
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-clp-level-2' }">
                  Level 2
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
        <li>
          <Accordion
            id="facial-dysostosis-nav"
            title="Facial Dysostosis"
            :isOpenByDefault="true"
          >
            <ul>
              <li>
                <router-link
                  :to="{ name: 'provider-facial-dysostosis-level-1' }"
                >
                  Level 1
                </router-link>
              </li>
              <li>
                <router-link
                  :to="{ name: 'provider-facial-dysostosis-level-2' }"
                >
                  Level 2
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
        <li>
          <Accordion
            id="craniofacial-microsomia-nav"
            title="Craniofacial Microsomia"
            :isOpenByDefault="true"
          >
            <ul>
              <li>
                <router-link
                  :to="{ name: 'provider-craniofacial-microsomia-level-1' }"
                >
                  Level 1
                </router-link>
              </li>
              <li>
                <router-link
                  :to="{ name: 'provider-craniofacial-microsomia-level-2' }"
                >
                  Level 2
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
        <li>
          <Accordion
            id="ghl-nav"
            title="Genetic hearing loss"
            :isOpenByDefault="true"
          >
            <ul>
              <li>
                <router-link :to="{ name: 'provider-ghl-your-center' }">
                  Your center's overview
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'provider-ghl-all-centers' }">
                  All centers
                </router-link>
              </li>
            </ul>
          </Accordion>
        </li>
      </ul>
    </nav>
    <a id="btnSubmitPatient" href="../tables/#/Subject">
      <span>Submit patient</span>
      <ArrowUpTrayIcon />
    </a>
  </aside>
</template>

<style lang="css">
.sidebar {
  background-color: white;
  box-sizing: content-box;
  padding: 2em 1.5em;
  box-shadow: var(--box-shadow);
  border-radius: 8px;

  --link-font-size: 12pt;
}

.sidebar nav {
  padding: 0;
  margin-top: 1em;
  margin-bottom: 1.5em;
}

.sidebar nav ul,
.sidebar nav ul li ul {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.sidebar nav .accordion {
  margin: 0;
  border: none;
}

.sidebar nav .accordion .accordion-heading {
  padding: 0;
  background-color: transparent;
}

.sidebar nav .accordion .accordion-heading .toggle-label {
  font-size: var(--link-font-size);
}

.sidebar nav .accordion .accordion-content {
  padding: 0;
}

.sidebar nav li {
  margin: 0.6em 0;
  border-left: 3px solid transparent;
  padding-left: 1em;
}

.sidebar nav li a,
.sidebar nav li p {
  font-size: var(--link-font-size);
  margin: 0;
  line-height: 1;
}

.sidebar nav li p {
  text-transform: uppercase;
  font-size: 11pt;
  font-weight: 400;
  letter-spacing: 0.08em;
  margin-top: 24px;
  padding-left: 1.5em;
  color: var(--gray-400);
}

.sidebar nav li a {
  color: currentColor;
}

.sidebar nav li a:hover,
.sidebar nav li a:focus {
  color: var(--cranio-primary);
}

.sidebar nav li.link-selected {
  border-left-color: var(--cranio-primary);
}
.sidebar nav li.link-selected a {
  color: var(--cranio-primary);
}

#btnSubmitPatient {
  display: block;
  width: 100%;
  padding: 0.6em 0;
  text-align: center;
  color: white;
  background-color: var(--cranio-orange);
  border-radius: 32pt;
}

#btnSubmitPatient span {
  text-transform: uppercase;
  font-size: 11pt;
  letter-spacing: 0.15em;
  border-bottom: 2px solid transparent;
}

#btnSubmitPatient:hover,
#btnSubmitPatient:focus {
  text-decoration: none;
  filter: brightness(110%);
}

#btnSubmitPatient svg {
  position: relative;
  width: 18px;
  margin-left: 4px;
  margin-top: -4px;
}

#btnSubmitPatient svg path {
  stroke-width: 2;
}
</style>
