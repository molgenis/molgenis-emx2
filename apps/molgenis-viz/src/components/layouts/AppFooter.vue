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
                  <MolgenisLogo />
                </a>
              </li>
            </ul>
          </nav>
        </div>
      </div>
    </div>
  </footer>
</template>

<script lang="ts" setup>
import MolgenisLogo from "../display/MolgenisLogo.vue";
const props = withDefaults(
  defineProps<{
    firstColumnTitle?: string;
    secondColumnTitle?: string;
    showProjectCitation?: boolean;
  }>(),
  {
    firstColumnTitle: "My Project",
    secondColumnTitle: "For members",
    showProjectCitation: false,
  }
);
</script>

<style lang="scss">
.app-footer {
  display: grid;
  grid-template-columns: 1fr;
  margin-bottom: 1em;

  .footer-content {
    box-sizing: content-box;
    margin: 0 auto;

    @media (min-width: 917px) {
      max-width: var(--max-width);
    }
  }

  .footer-list-title {
    text-transform: uppercase;
    font-size: 11pt;
    font-weight: 800;
    letter-spacing: 0.08em;
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
    background-color: var(--gray-050);

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
            text-transform: uppercase;
            font-size: 11pt;
            font-weight: 400;
            letter-spacing: 0.08em;
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
}
</style>
