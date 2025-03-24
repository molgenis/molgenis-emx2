<template>
  <footer class="app-footer">
    <div class="links-container">
      <div class="footer-content">
        <div class="footer-links">
          <p id="column-links-1-title" class="footer-list-title">
            {{ firstColumnTitle }}
          </p>
          <nav aria-labelledby="column-links-1-title">
            <ul class="footer-list">
              <!-- space for additional links to appear in column 1 -->
              <slot name="column-links-1"></slot>
            </ul>
          </nav>
        </div>
        <div class="footer-links">
          <p id="column-links-2-title" class="footer-list-title">
            {{ secondColumnTitle }}
          </p>
          <nav aria-labelledby="column-links-2-title">
            <ul class="footer-list">
              <!-- space for additional links to appear in column 2 -->
              <slot name="column-links-2"></slot>
            </ul>
          </nav>
        </div>
        <div class="footer-logos">
          <p id="column-logos-title" class="footer-list-title visually-hidden">
            affiliated projects and partners
          </p>
          <nav aria-labelledby="column-logos-title">
            <ul class="footer-list">
              <!-- space for additional logos to appear in column 3 -->
              <slot name="column-logos"></slot>
              <li>
                <a href="https://www.molgenis.org">
                  <span class="visually-hidden">
                    visit the molgenis website to learn more
                  </span>
                  <img
                    src="/molgenis-logo-blue-text.png"
                    class="molgenis-logo"
                    alt="molgenis open source data platform"
                  />
                </a>
              </li>
            </ul>
          </nav>
        </div>
      </div>
    </div>
    <div class="citation-container" v-if="showProjectCitation">
      <div class="footer-content">
        <nav>
          <ul class="footer-list list-horizontal">
            <slot name="site-citation"></slot>
          </ul>
        </nav>
      </div>
    </div>
    <div class="molgenis-meta">
      <div class="footer-content">
        <p>
          This database was created using
          <a href="https://www.molgenis.org/">MOLGENIS open source software</a>
          <span v-if="manifest?.SpecificationVersion">
            using version {{ manifest?.SpecificationVersion }}
          </span>
        </p>
        <p>
          Please cite
          <a
            href="https://www.ncbi.nlm.nih.gov/pubmed/30165396"
            data-v-4613be74=""
          >
            Van der Velde et al (2018)</a
          >
          or
          <a
            href="https://www.ncbi.nlm.nih.gov/pubmed/21210979"
            data-v-4613be74=""
          >
            Swertz et al (2010)</a
          >
          on use.
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { ref, onBeforeMount } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";

withDefaults(
  defineProps<{
    //  @param firstColumnTitle Set the title of the first set of navigation links
    firstColumnTitle?: string;

    // Set the title of the second set of navigation links
    secondColumnTitle?: string;

    // If True (default), the footer area for project citations
    showProjectCitation?: boolean;
  }>(),
  {
    firstColumnTitle: "My Project",
    secondColumnTitle: "For Members",
    showProjectCitation: true,
  }
);

interface IManifestSelection {
  SpecificationVersion?: string;
  ImplementationVersion?: string;
}

interface IManifestResponse {
  _manifest?: IManifestSelection;
}

const manifest = ref<IManifestSelection>();

async function getManifest() {
  const query = gql`
    {
      _manifest {
        ImplementationVersion
        SpecificationVersion
      }
    }
  `;
  const response: IManifestResponse = await request("/api/graphql", query);
  manifest.value = response._manifest;
}

onBeforeMount(() => getManifest());
</script>

<style lang="scss">
.app-footer {
  display: grid;
  grid-template-columns: 1fr;

  .footer-content {
    box-sizing: content-box;
    margin: 0 auto;

    @media (min-width: 917px) {
      max-width: $max-width;
    }
  }

  .footer-list-title {
    @include textTransform(bold);
    color: $gray-050;
  }

  .footer-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      margin-bottom: 0.4em;
      color: $gray-050;
    }

    &.list-horizontal {
      display: flex;
      justify-content: center;
      align-items: center;

      li {
        padding: 0.2em 0.1em;
        margin-bottom: 0;

        @media (min-width: 917px) {
          &::after {
            content: "|";
            display: inline-block;
            width: 2em;
            color: currentColor;
          }

          &:last-child {
            &::after {
              display: none;
            }
          }
        }
      }
    }
  }

  .links-container {
    background-color: $gray-800;

    .footer-content {
      display: grid;
      grid-template-columns: 1fr;
      gap: 1em;
      padding: 2em;

      @media (min-width: 517px) {
        grid-template-columns: repeat(2, 1fr);
      }

      @media (min-width: 917px) {
        grid-template-columns: repeat(3, 1fr);
      }

      .footer-links {
        .footer-list {
          a {
            @include textTransform;
            text-decoration: none;
            padding-bottom: 2px;
            border-bottom: 2px solid transparent;
            color: currentColor;

            &:hover,
            &:focus {
              border-bottom-color: currentColor;
            }
          }
        }
      }
    }

    .footer-logos {
      .footer-list {
        li {
          margin-bottom: 1.3em;
        }
      }
    }
  }

  .citation-container {
    background-color: $gray-050;
    width: 100%;
    text-align: center;
    .footer-list {
      li {
        a {
          @include textTransform;
          color: $gray-700;
          font-size: 0.8rem;
        }
      }
    }
  }

  .molgenis-meta {
    padding: 1em;
    text-align: center;
    background-color: $gray-000;
    font-size: 0.9rem;
    p {
      margin: 0;
    }
  }
}
</style>
