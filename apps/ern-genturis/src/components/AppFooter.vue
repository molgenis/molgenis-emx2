<template>
  <footer class="app-footer">
    <div class="content-container links-container">
      <div class="footer-area footer-content">
        <div class="footer-column footer-links">
          <p id="column-links-1-title" class="footer-list-title">{{ firstColumnTitle }}</p>
          <nav aria-labelledby="column-links-1-title">
            <ul class="footer-list">
            <!-- space for additional links to appear in column 1 -->
              <slot name="column-links-1"></slot>
            </ul>
          </nav>
        </div>
        <div class="footer-column footer-links">
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
        <div class="footer-column footer-logos">
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
                    src="/apps/molgenis-components/assets/img/molgenis_logo.png"
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
    <div class="footer-content-container footer-citation-container" v-if="showProjectCitation">
      <div class="footer-area footer-content">
        <nav>
          <ul class="footer-list list-horizontal"> 
            <slot name="site-citation"></slot>
          </ul>
        </nav>
      </div>
    </div>
    <div class="footer-content-container molgenis-meta" v-if="showMolgenisMeta">
      <div class="footer-area">
        <p>
          This database was created using
          <a href="https://www.molgenis.org/">MOLGENIS open source software</a>
          <span v-if="manifest.SpecificationVersion">
            using version {{ manifest.SpecificationVersion }}
          </span>
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";

const props = defineProps({
  
  // Set the title of the first set of navigation links
  firstColumnTitle: {
    type: String,
    default: "My Project"
  },
  
  // Set the title of the second set of navigation links
  secondColumnTitle: {
    type: String,
    default: "For members",
  },
  
  // If True (default), the footer area for project citations
  // will be shown
  showProjectCitation: {
    type: Boolean,
    default: false
  },
  
  // If True (default), metadata about your molgenis instance will
  // be displayed.
  showMolgenisMeta: {
    type: Boolean,
    default: true,
  },
});

async function getManifest() {
  const query = gql`
    {
      _manifest {
        ImplementationVersion
        SpecificationVersion
      }
    }
  `;
  const response = await request("/api/graphql", query);
  return response._manifest;
}

const manifest = computed(() => {
  return props.showMolgenisMeta ? getManifest() : {};
});
</script>

<style lang="scss">
.app-footer {
  display: grid;
  grid-template-columns: 1fr;
  
  .content-container {
    width: 100%;
  }
  
  .footer-area { 
    box-sizing: content-box;
    margin: 0 auto;
    width: 100%;
    flex-grow: 1;    
    
    p {
      margin-bottom: 0;
    }

    @media (min-width: 917px) {
      max-width: $max-width;
    }
  }

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
    
    .footer-list-title {
      @include textTransform(bold);
      margin-bottom: 0.6em;
    }
    
    .footer-list {
      list-style: none;
      padding: 0;
      margin: 0;
      
      li {
        margin-bottom: 0.4em;   
      }
      
      &.list-horizontal {
        display: flex;
        justify-content: center;
        align-items: center;
        border: 1px solid red;

        li {

          flex-grow: 1;
          margin-bottom: 0;
        }
      }
    }
    
    .footer-links {
      .footer-list {
        a {
          @include textTransform;
          text-decoration: none;
          padding-bottom: 2px;
          border-bottom: 2px solid transparent;
          color: currentColor;
        
          &:hover, &:focus {
            border-bottom-color: currentColor;
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
  
  .links-container {
    background-color: $gray-050;
  }

  .molgenis-meta {
    padding: 1em;
    text-align: center;
    background-color: $gray-000;
    font-size: 0.9rem;
  }
}
</style>
