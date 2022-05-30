<template>
  <div class="">
    <div class="row">
      <div class="col">
        <BannerImage
          imageUrl="https://image.focuspoints.io/general-1.jpg?_jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJmb2N1c1BvaW50WSI6MC4wLCJmb2N1c1BvaW50WCI6MC4wLCJ3aWR0aCI6MTQ0MCwiaXNzIjoidW1jZyIsImFjdGlvbiI6InRyYW5zZm9ybSIsInVybCI6Imh0dHBzOi8vdW1jZ3Jlc2VhcmNoLm9yZy9kb2N1bWVudHMvNzcwNTM0Lzc3NTkwNy9nZW5lcmFsLTEuanBnL2NlNGVkMTM5LTMxYzMtOTg3Mi02NjBiLWU3ZjVjNDFhNTY0OD90PTE2Mjk3MDYzODQyOTMmZG93bmxvYWQ9dHJ1ZSIsImhlaWdodCI6MzYwfQ.ulaZWsVt6k6Uil4zLdaxpnLrWZJubDttUIlE5hr5yqgXW7ACAD5nF1Kpl4R-Wd2QU2haLYJt0zvzMWv2843gfA"
          title="Cohorts, biobanks and dataset of the UMC"
          subTitle="Universitair Medisch Centrum Groningen, the Netherlands"
        />
      </div>
    </div>

    <div class="row justify-content-md-center py-3">
      <div class="col-sm-12 col-md-8">
        <search-resource :resourceType="resourceType" />
      </div>
    </div>

    <div class="row">
      <div class="col">
        <div class="card-deck">
          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <DatabaseIcon :size="iconSize" class="mg-card-icon" /> Datasets
              </h5>
              <div class="card-text">
                <h1 class="text-center">1,26M</h1>
                <ul class="card-text">
                  <li>Cohorts: 89</li>
                  <li>Biobanks: 15</li>
                </ul>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted">Last updated 3 mins ago</small>
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <UsersIcon :size="iconSize" class="mg-card-icon" />Participants
              </h5>

              <div class="card-text">
                <h1 class="text-center">1,26M</h1>
                <p>in total > 1,000: 34%</p>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted">Last updated 3 mins ago</small>
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <TestPipeIcon :size="iconSize" class="mg-card-icon" />Samples
              </h5>
              <div class="card-text">
                <h1 class="text-center">61%</h1>
                <ul>
                  <li>Blood: 54%</li>
                  <li>DNA: 28%</li>
                  <li>Other: 44%</li>
                </ul>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted">Last updated 3 mins ago</small>
            </div>
          </div>

          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <ToolsIcon :size="iconSize" class="mg-card-icon" />Cohort design
              </h5>
              <div class="card-text">
                <h1 class="text-center">61%</h1>
                <ul>
                  <li>Prospective: 54%</li>
                  <li>Retrospective: 28%</li>
                  <li>Both: 44%</li>
                </ul>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted">Last updated 3 mins ago</small>
            </div>
          </div>

          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <ClockIcon :size="iconSize" class="mg-card-icon" />Longitudinal
                studies
              </h5>
              <div class="card-text">
                <h1 class="text-center">77,5%</h1>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted">Last updated 3 mins ago</small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row mt-3">
      <div class="col">
        <div class="card-deck">
          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <CirclePlusIcon :size="iconSize" class="mg-card-icon" />
                Recently added
              </h5>
              <div class="card-text">
                <ul class="card-text">
                  <li>23-04-2022 - Head and neck oncology patients</li>
                  <li>
                    12-01-2022 - Lung path ( Lung tissue and cell biobanks
                  </li>
                  <li>22-12-2021 - Oncolifes</li>
                </ul>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted"></small>
            </div>
          </div>

          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <NewsIcon :size="iconSize" class="mg-card-icon" /> News
              </h5>
              <div class="card-text">
                <ul class="card-text">
                  <li>
                    2021-03-01 – The dataset of TRAILS-CC is now available for
                    external users
                  </li>
                  <li>2021-03-01 - Bla die bla</li>
                </ul>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted"></small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row mt-3">
      <div class="col">
        <div class="card-deck">
          <div class="card">
            <div class="card-body">
              <h5 class="card-title">
                <AffiliateIcon :size="iconSize" class="mg-card-icon" />
                Cohort & Biobank Coordination hub
              </h5>
              <div class="card-text">
                <address>
                  <strong>University Medical Center Groningen (UMCG)</strong
                  ><br />
                  Antonius Deusinglaan 1<br />
                  9713 AV Groningen<br />
                  The Netherlands<br />
                  <abbr title="Phone"></abbr> (123) 456-7890
                </address>
              </div>
            </div>
            <div class="card-footer">
              <small class="text-muted"></small>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import SearchResource from "../components/SearchResource";
import BannerImage from "../components/display/BannerImage.vue";

import {
  DatabaseIcon,
  UsersIcon,
  TestPipeIcon,
  CirclePlusIcon,
  NewsIcon,
  ToolsIcon,
  ClockIcon,
  AffiliateIcon,
} from "vue-tabler-icons";

export default {
  name: "HomeView",
  components: {
    SearchResource,
    BannerImage,
    DatabaseIcon,
    UsersIcon,
    TestPipeIcon,
    CirclePlusIcon,
    ToolsIcon,
    NewsIcon,
    ClockIcon,
    AffiliateIcon,
  },
  data() {
    return {
      iconSize: "62",
    };
  },
  props: {
    resourceType: {
      type: String, // one of Resource
      default: () => "Institutions",
    },
  },
};
</script>

<style scoped>
.mg-card-icon {
  color: var(--primary)
}
</style>
