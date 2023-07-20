<template>
  <PageFooter>
    <div class="footer-container width-full">
      <div class="footer-content width-medium">
        <div class="footer-column footer-links">
          <p><strong>ERN-Skin</strong></p>
          <UnorderedList listType="none">
            <li><router-link :to="{name: 'home'}">Home</router-link></li>
            <li><router-link :to="{name: 'about-us'}">About Us</router-link></li>
            <li><router-link :to="{name: 'documents'}">Documents</router-link></li>
            <li><router-link :to="{name: 'dashboard'}">Dashboard</router-link></li>
            <!-- more links here -->
          </UnorderedList>
        </div>
        <div class="footer-column footer-links">
          <p><strong>For Members</strong></p>
          <UnorderedList listType="none">
            <li><a href="/login">Sign in</a></li>
            <!-- more links here -->
          </UnorderedList>
        </div>
        <div class="footer-column footer-logos">
          <UnorderedList listType="none">
            <li id="project-logo-link">
              <router-link :to="{ name: 'home' }"> 
                <img :src="ProjectLogo" alt="ERN-Skin" class="ern-skin-logo" />
              </router-link>
            </li>
            <li class="eu-logos">
              <img :src="ErnLogo" class="logo ern-logo" />
              <img :src="FundingLogo" class="logo funding-logo" alt="funded by the European Union" />
            </li>
            <li>
              <a href="https://www.molgenis.org">
                <span class="visually-hidden">visit the molgenis website to learn more</span>
                <li><img :src="MolgenisLogo" class="molgenis-logo" alt="molgenis open source data platform"/></li>
              </a>
            </li>
          </UnorderedList>
        </div>
      </div>
    </div>
  </PageFooter>
</template>


<script setup>
import { PageFooter, UnorderedList } from 'molgenis-viz';
import ProjectLogo from "../assets/ern-reconnet-logo.png";
import MolgenisLogo from "../assets/molgenis-logo-blue-text.png";
import ErnLogo from "../assets/ern-reconnet-header.jpg";
import FundingLogo from "../assets/ern-funded-by-eu.svg";
</script>

<style lang="scss">
.page-footer {
  background-color: $gray-050;
  
  .footer-container {
    color: $gray-050;
    background-color: $blue-900;
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
    
    a {
      @include textTransform;
      text-decoration: none;
      padding-bottom: 2px;
      border-bottom: 2px solid transparent;
    
      &:hover, &:focus {
        border-bottom-color: currentColor;
      }
    }
  }
  
  .page-footer-meta-links {
    box-sizing: padding-box;
    padding: 0.2em;
    // background-color: $brand-red-orange;
    
    a {
      @include textTransform;
      text-decoration: none;
      padding-bottom: 2px;
      border-bottom: 2px solid transparent;
    
      &:hover, &:focus {
        border-bottom-color: currentColor;
      }
    }
  }
  
  .molgenis-citation {
    background-color: $gray-050;
  }
}

</style>