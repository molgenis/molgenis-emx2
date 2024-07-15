<template>
  <Page id="page-home">
    <CustomPageHeader
      class="erras-header"
      title="ERN-Skin Registry"
      subtitle="Registry for Rare and Undiagnosed Skin Diseases"
      imageSrc="erras-header.jpg"
      height="xlarge"
      title-position-x="center"
      title-position-y="center"
    />
    <PageSection
      id="section-welcome"
      aria-labelledby="section-welcome-title"
      :verticalPadding="2"
    >
      <h2 id="section-welcome-title">Welcome to the ERN-Skin registry!</h2>
      <p>
        The general objective of this proposal is to build a central registry
        for all rare skin diseases, connected with the existing registries and
        including patients with undiagnosed cases.
      </p>
    </PageSection>
    <PageSection
      class="quick-links bg-blue-100"
      aria-labelledby="quicklinks-card-title"
      :verticalPadding="2"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">
        quick links: learn more about ERN Skin
      </h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about">
          <InformationCircleIcon />
          <h3>About</h3>
          <p>Learn more about the ERRAS registry</p>
          <router-link :to="{ name: 'about' }">Read more</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <PresentationChartLineIcon />
          <h3>Registry Dashboard</h3>
          <p>View a summary of the entire registry</p>
          <router-link :to="{ name: 'dashboard' }">View Dashboard</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <DocumentTextIcon />
          <h3>View Documents</h3>
          <p>Find and download ERRAS documents</p>
          <router-link :to="{ name: 'documents' }">View Documents</router-link>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection
      id="section-aims"
      class="bg-gray-050"
      aria-lablledby="section-aims-title"
      :verticalPadding="2"
    >
      <h2 id="section-aims-title">Aims of the ERN-Skin registry</h2>
      <p>
        The <strong>ERN-Skin</strong> gathers European medical experts and
        patient representatives who join their efforts to improve the diagnosis
        and management of all the patients suffering from rare, complex and
        undiagnosed skin diseases all over Europe.
      </p>
      <p>
        Structured in 9 Disease Groups and Transversal thematic Groups, ERN-Skin
        has been developing an innovative e-health platform allowing the
        construction of a common Skin Diseases registry: ERRAS, a key instrument
        for our ERN.
      </p>
      <!-- insert image: ern-skin box diagram -->
      <img
        src="/ern-skin-groups.png"
        class="project-diagrams"
        alt="ERN-Skin disease groups"
      />
      <p>
        The SKIN ePAGs bring together rare skin disease patient representatives
        who are actively involved in the work of the ERN-Skin.
      </p>
      <p>
        ERRAS, the European registry for rare dermatologic diseases receives
        information from existing European registries as well as additional
        manual feeds. It is a means to put scientists and patients together via
        their respective healthcare providers.
      </p>
      <!-- insert image: ERDI image -->
      <img
        src="/ern-skin-erdri.png"
        class="project-diagrams"
        alt="European registry for rare dermatologic diseases"
      />
    </PageSection>
    <PageSection
      id="section-contact"
      aria-labelledby="section-contact-title"
      :verticalPadding="2"
    >
      <h2 id="section-contact-title">Contact</h2>
      <p>
        If you want to know more, to participate, or to consult the ERN-Skin
        registry, thanks to send your query to the following email address:
        <a href="mailto:skin-registry@ern-skin.eu>">
          skin-registry@ern-skin.eu
        </a>
        or at the following address.
      </p>
      <address class="address">
        <span><strong>Coordinating Center</strong></span>
        <span>Hôpital Necker Enfants Malades</span>
        <span>Department of Dermatology</span>
        <span>149 rue de Sèvres</span>
        <span>75015 Paris</span>
      </address>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageSection, LinkCard } from "molgenis-viz";
import {
  InformationCircleIcon,
  PresentationChartLineIcon,
  DocumentTextIcon,
} from "@heroicons/vue/24/outline";

import CustomPageHeader from "../components/CustomPageHeader.vue";
</script>

<style lang="scss">
.quicklink-cards {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 2em;

  .link-card {
    flex-grow: 1;
    padding: 1em;
    background-color: $gray-000;
    color: currentColor;
    border-radius: 12pt;
    text-align: center;

    .card-content {
      svg {
        display: block;
        margin: 0 auto;
        margin-bottom: 1.5em;
        @include setIconSize(26pt);
      }

      h3 {
        font-size: 1.1rem;
      }

      p {
        margin: 0;
        color: $gray-600;
        font-size: 1.1rem;
      }

      a {
        margin-top: 1em;
        font-size: 0.85rem;
      }
    }

    &:nth-child(1) {
      .card-content {
        svg {
          path {
            stroke: $erras-blue;
          }
        }

        a {
          @include setLinkAsButton($background-color: $erras-blue);
        }
      }
    }

    &:nth-child(2) {
      .card-content {
        svg {
          path {
            stroke: $erras-green;
          }
        }

        a {
          @include setLinkAsButton($background-color: $erras-green);
        }
      }
    }

    &:nth-child(3) {
      .card-content {
        svg {
          path {
            stroke: $erras-orange;
          }
        }

        a {
          @include setLinkAsButton($background-color: $erras-orange);
        }
      }
    }
  }

  @media (min-width: 936px) {
    flex-wrap: nowrap;
  }
}
</style>
