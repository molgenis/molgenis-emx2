<template>
  <PageFooter>
    <div class="footer-container width-full">
      <div class="footer-content width-medium">
        <div class="footer-column footer-links">
          <p><strong>ERN CRANIO</strong></p>
          <UnorderedList listType="none">
            <li>
              <a href="/CranioStats/cranio-public/#/">Home</a>
            </li>
            <li>
              <a href="/CranioStats/cranio-public/#/about">About</a>
            </li>
            <li>
              <a href="/CranioStats/cranio-public/#/dashboard">Dashboard</a>
            </li>
          </UnorderedList>
        </div>
        <div class="footer-column footer-links">
          <p><strong>For Members</strong></p>
          <UnorderedList listType="none">
            <li>
              <a href="/CranioStats/cranio-public/#/Providers">Providers</a>
            </li>
            <li>
              <a href="/CranioStats/cranio-public/#/Documents">Documents</a>
            </li>
          </UnorderedList>
        </div>
        <div class="footer-column footer-logos">
          <UnorderedList listType="none">
            <li id="project-logo-link">
              <a href="/CranioStats/cranio-public/#/">
                <img
                  src="/ern-cranio-logo.png"
                  alt="ERN CRANIO: European Reference Network for rare and/or complex craniofacial anomalies and ear, nose and throat (ENT) disorders"
                />
              </a>
            </li>
            <li>
              <a href="https://www.molgenis.org">
                <span class="visually-hidden">
                  visit the molgenis website to learn more
                </span>
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
    background-color: $gray-000;
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
