<template>
  <PageFooter>
    <div class="footer-container width-full">
      <div class="footer-content width-medium">
        <div class="footer-column footer-links">
          <p><strong>ERN ReCONNET</strong></p>
          <UnorderedList listType="none">
            <li>
              <router-link :to="{ name: 'ern-reconnet-home' }"
                >Home</router-link
              >
            </li>
            <li>
              <router-link :to="{ name: 'ern-reconnet-about' }"
                >About Us</router-link
              >
            </li>
            <li>
              <router-link :to="{ name: 'ern-reconnet-dashboard' }"
                >Dashboard</router-link
              >
            </li>
            <!-- more links here -->
          </UnorderedList>
        </div>
        <div class="footer-column footer-links">
          <p><strong>For Members</strong></p>
        </div>
        <div class="footer-column footer-logos">
          <UnorderedList listType="none">
            <li id="project-logo-link">
              <router-link :to="{ name: 'ern-reconnet' }">
                <img
                  src="/ern-reconnet-logo.png"
                  alt="ERN-RECONNET registry"
                  class="ern-logo"
                />
              </router-link>
            </li>
            <li class="eu-logos">
              <img
                src="/ern-funded-by-eu.svg"
                class="logo funding-logo"
                alt="funded by the European Union"
              />
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
</script>

<style lang="scss">
.page-footer {
  background-color: $gray-050;

  .footer-container {
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
    flex-grow: 2;

    .toggle {
      background: none;
      border: none;
    }

    a,
    .toggle {
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

  .page-footer-meta-links {
    box-sizing: padding-box;
    padding: 0.2em;

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
    background-color: $gray-050;
  }
}
</style>
