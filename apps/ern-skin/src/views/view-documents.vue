<template>
  <Page id="page-documents">
    <CustomPageHeader
      class="erras-header"
      title="ERN-Skin Registry"
      subtitle="Additional Information"
      imageSrc="img/erras-header.jpg"
      height="xlarge"
      title-position-x="center"
      title-position-y="center"
    />
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="2"
    >
      <h2 id="section-documents-title"></h2>
        <Accordion
            id="studies-nav"
            title="Studies"
            :isOpenByDefault="false"
            :headingStyle="1"
            >
        <ul>
          <Studies table="Studies" labelsColumn="title" />
        </ul>
        </Accordion>
        <Accordion
            id="accReq-nav"
            title="Access Requests"
            :isOpenByDefault="false"
            :headingStyle="2"
            >
        <ul>
          <FileList table="Files" labelsColumn="name" fileColumn="file" />
        </ul>
        </Accordion>
        <Accordion
            id="patient-nav"
            title="Are you a patient?"
            :isOpenByDefault="false"
            :headingStyle="3"
            >
        <ul>
          <li>
            Informed consent: please check with your treating doctor who will
            provide you with all explanations and documents in local language.
          </li>
          <li>
            Withdrawal of consent: please check with your treating doctor who
            will provide you with the information for the withdrawal of your
            consent.
          </li>
          <li>Contact point: skin-registry@ern-skin.eu</li>
        </ul>
        </Accordion>
        <Accordion
            id="general-nav"
            title="General Documents"
            :isOpenByDefault="false"
            :headingStyle="4"
            >
        <ul>
          <big><strong>Publications</strong></big>
          <Publications
            table="Publications"
            labelsColumn="title"
            doiColumn="doi"
          />
        </ul>
        </Accordion>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Accordion, Page, PageSection, FileList } from "molgenis-viz";
import CustomPageHeader from "../components/CustomPageHeader.vue";
import Publications from "../components/Publications.vue";
import Studies from "../components/Studies.vue";
import { ref } from "vue";
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
    margin-bottom: 1.5em;

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
          color: blue;
        }
      }

      &.link-selected {
        border-left-color: blue;
        a {
          color: blue;
        }
    }
  }
}

  svg {
    position: relative;
    width: 18px;
    margin-left: 4px;
    margin-top: -4px;
    path {
      stroke-width: 2;
    }
  }
}
</style>
