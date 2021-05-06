<template>
<div>
  <table class="table table-sm table-hover">
    <thead>
      <tr>
        <th scope="col"></th>
        <th class="rotated-text text-nowrap" scope="col" v-for="(databank) in databanks" :key=databank.acronym >
          <div>
            <span>{{databank.acronym}}</span>
          </div>
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(variable) in variables" :key=variable.name >
        <th class="text-nowrap" scope="row">{{variable.name}}</th>
        <td 
          v-for="(databank) in databanks" :key=databank.acronym 
          :class="'table-'+getMatchStatus(variable.name, databank.acronym)" @click="mappingDetails(variable.name, databank.acronym)" >
        </td>
      </tr>
    </tbody>
  </table>
</div>
</template>

<script>
import { mapGetters, mapActions, mapState } from 'vuex'

export default {
  name: "MappingView",
  computed: {
    ...mapGetters(['mappings', 'getMapping']),
    ...mapState(['databanks', 'variables'])
  },
  methods: {
    ...mapActions(['fetchCohorts', 'fetchMappings']),
    getMatchStatus(variableName, databankAcronym) {
      if(!this.mappings[variableName] || !this.mappings[variableName][databankAcronym]) {
        return 'danger' // not mapped
      }

      const match = this.mappings[variableName][databankAcronym]
      switch(match) {
        case 'zna':
        case 'zna':
          return 'danger'
        case 'partial':
          return 'warning'
        case 'complete':
          return 'success'
        default:
          return 'danger'
      }
    },
    mappingDetails(variable, databank) {
      const mapping = this.getMapping(variable, databank)
      this.$router.push({name: 'MappingDetailView', path: '/mapping/detail', params: { mapping: mapping }}) 
    }
  },
  watch: {
    variables () {
      this.fetchMappings()
    }
  },
  mounted () {
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
