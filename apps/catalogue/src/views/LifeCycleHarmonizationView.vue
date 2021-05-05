<template>
<div>
  <table class="table table-sm table-hover">
    <thead>
      <tr>
        <th scope="col"></th>
        <th class="rotated-text text-nowrap" scope="col" v-for="(cohort) in cohorts" :key=cohort.acronym >
          <div>
            <span>{{cohort.acronym}}</span>
          </div>
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(variable) in variables" :key=variable.name >
        <th class="text-nowrap" scope="row">{{variable.name}}</th>
        <td 
          v-for="(cohort) in cohorts" :key=cohort.acronym 
          :class="'table-'+getMatchStatus(variable.name, cohort.acronym)">
          <!-- {{getMatchStatus(variable.name, cohort.acronym)}} -->
        </td>
      </tr>
    </tbody>
  </table>
</div>
</template>

<script>
import { mapGetters, mapActions, mapState } from 'vuex'

export default {
  name: "LifeCycleHarmonizationView",
  computed: {
    ...mapGetters(['harmonizationGrid']),
    ...mapState(['cohorts', 'variables'])
  },
  methods: {
    ...mapActions(['fetchCohorts', 'fetchMappings']),
    getMatchStatus(variableName, cohortAcronym) {
      // mock status filling
      // return  ['success', 'danger', 'warning'][Math.floor(Math.random() * 3)];

      if(!this.harmonizationGrid[variableName] || !this.harmonizationGrid[variableName][cohortAcronym]) {
        return 'na' // not mapped
      }

      return this.harmonizationGrid[variableName][cohortAcronym]
    }
  },
  mounted () {
    this.fetchCohorts()
    this.fetchMappings()
  }
}
</script>

<style scoped>
th.rotated-text {
    height: 140px;
    padding: 0 !important;
}

th.rotated-text > div {
    transform:
        /* translate(13px, 0px) */
        rotate(300deg);
    width: 1rem;
}

th.rotated-text > div > span {
    padding: 5px 10px;
}

</style>
