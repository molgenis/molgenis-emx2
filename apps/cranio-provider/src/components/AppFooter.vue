<template>
  <PageFooter>
    <div class="footer-container width-full">
      <div class="footer-content width-medium">
        <div class="footer-column footer-links">
          <p><strong>ERN CRANIO</strong></p>
          <UnorderedList listType="none">
            <li><router-link :to="{ name: 'home' }">Home</router-link></li>
            <li><router-link :to="{ name: 'about' }">About</router-link></li>
            <li>
              <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
            </li>
            <!-- more links here -->
          </UnorderedList>
        </div>
        <div class="footer-column footer-links">
          <p><strong>For Members</strong></p>
          <UnorderedList listType="none">
            <li>
              <router-link :to="{ name: 'providers', params: { id: orgId } }">
                Member Sign in
              </router-link>
            </li>
            <!-- more links here -->
          </UnorderedList>
        </div>
        <div class="footer-column footer-logos">
          <UnorderedList listType="none">
            <li id="project-logo-link">
              <router-link :to="{ name: 'home' }">
                <!-- insert project logo here -->
                <img
                  src="/ern-cranio-logo.png"
                  alt="ERN CRANIO: European Reference Network for rare and/or complex craniofacial anomalies and ear, nose and throat (ENT) disorders"
                />
              </router-link>
            </li>
            <li>
              <a href="https://www.molgenis.org">
                <span class="visually-hidden"
                  >visit the molgenis website to learn more</span
                >
                <li>
                  <img
                    src="/molgenis-logo-blue-text.png"
                    class="molgenis-logo"
                    alt="molgenis open source data platform"
                  />
                </li>
              </a>
            </li>
          </UnorderedList>
        </div>
      </div>
    </div>
  </PageFooter>
</template>

<script setup>
import { PageFooter, UnorderedList } from "molgenis-viz";
import viewProps from "../data/props";
const props = defineProps(viewProps);
</script>

<style lang="scss">
.page-footer {
  background-color: $gray-000;

  .footer-container {
    color: $gray-800;
    background-color: $gray-050;
  }

  .footer-content {
    box-sizing: padding-box;
    padding: 2em 5em;
    display: flex;
    justify-content: flex-start;
    align-items: flex-start;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 2em;
    margin: 0 auto;
  }

  .footer-links {
    position: relative;
    flex-grow: 2;

    a {
      @include linkTextTransform;
      text-decoration: none;
      padding-bottom: 2px;
      border-bottom: 2px solid transparent;

      &:hover,
      &:focus {
        border-bottom-color: currentColor;
      }
    }
    .heroicons {
      position: absolute;
      top: 0;
      margin-left: 3px;
      $size: 15px;
      width: $size;
      height: $size;
    }
  }

  .page-footer-meta-links {
    box-sizing: padding-box;
    padding: 0.8em;
    background-color: $blue-200;

    a {
      @include textTransform;
      text-decoration: none;
      padding-bottom: 2px;
      border-bottom: 2px solid transparent;

      &:hover,
      &:focus {
        border-bottom-color: currentColor;
      }
    }
  }

  .molgenis-citation {
    background-color: $gray-000;
  }
}
</style>
