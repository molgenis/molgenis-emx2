<template>
  <div class="container">
    <div class="col-12>">
      <footer class="bottom-0 p-0 w-100 text-primary-text">
        <hr class="mt-10 mb-10" />
        <div class="row d-flex flex-column flex-md-row">
          <div class="col text-center mb-4 mb-md-0">
            <a href="https://bbmri-eric.eu">
              <img
                width="auto"
                height="40"
                src="/img/bbmri-eric-logo.png"
                alt="logo negotiator"
              />
            </a>
          </div>
          <div class="col text-center">
            <span> Follow Us: </span>
            <a
              v-if="isFooterFollowUsVisible"
              href="https://www.linkedin.com/company/bbmri-eric/mycompany"
              class="link-dark ms-2"
            >
              <i class="bi bi-linkedin mr-3" />
            </a>
            <a
              v-if="isFooterFollowUsVisible"
              href="https://twitter.com/BBMRIERIC"
              class="link-dark ms-2"
            >
              <i class="bi bi-twitter-x mr-3" />
            </a>
            <a
              v-if="isFooterFollowUsVisible"
              href="https://www.bbmri-eric.eu/bbmri-eric/bbmri-eric-podcast/"
              class="link-dark ms-2"
            >
              <i class="bi bi-mic-fill" />
            </a>
            <div class="mt-2 mb-2 mb-md-0">
              <a
                href="https://github.com/molgenis/molgenis-emx2"
                class="text-primary-text ps-2"
              >
                <i class="bi bi-github" />
                GitHub
              </a>
            </div>

            <div v-if="isFooterFollowUsVisible" class="mt-2 mb-2 mb-md-0">
              <a
                href="https://status.bbmri-eric.eu/"
                class="text-primary-text ps-2"
              >
                <i class="bi bi-check-circle" />
                BBMRI-ERIC Status page
              </a>
            </div>
          </div>

          <div class="col text-center">
            <a
              class="link-dark text-primary-text"
              href="https://www.bbmri-eric.eu/wp-content/uploads/BBMRI-ERIC_work-program_2022-2024_DIGITAL.pdf"
            >
              <img
                width="22"
                height="22"
                src="/img/work-programme.png"
                alt="Work programme image"
              />
              Work Programme
            </a>
            <div v-if="isFooterFollowUsVisible" class="ms-md-3 mt-2">
              <a
                role="button"
                class="btn btn-light ms-md-5 text-primary-text"
                href="https://www.bbmri-eric.eu/news-event/"
              >
                Subscribe To Our Newsletter
              </a>
            </div>
            <div v-if="isFooterFollowUsVisible" class="ms-md-2 mt-2">
              <a
                class="link-dark text-primary-text me-5"
                href="https://www.bbmri-eric.eu/privacy-notice/"
              >
                Privacy Policy
              </a>
            </div>
          </div>
          <div>
            <div class="row mt-2">
              <div class="col text-center">
                <p>&copy; 2024 BBMRI-ERIC</p>
              </div>
              <div class="col text-center text-light ms-4"></div>
              <div class="col text-center ms-5">
                Need help?
                <a href="mailto:directory@helpdesk.bbmri-eric.eu">Contact us </a
                >.
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
const isFooterFollowUsVisible = ref<boolean>(true);
</script>

<style scoped>
a {
  text-decoration: none;
}
.row > * {
  flex-shrink: 0;
  width: 100%;
  max-width: 100%;
  padding-right: calc(var(--bs-gutter-x) * 0.5);
  padding-left: calc(var(--bs-gutter-x) * 0.5);
  margin-top: var(--bs-gutter-y);
}
</style>
