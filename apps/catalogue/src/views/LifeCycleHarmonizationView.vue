<template>
<div>
  <table class="table ">
    <thead>
      <tr>
        <th scope="col"></th>
        <th class="rotated-text text-nowrap" scope="col" v-for="(harmonization) in harmonizations" :key=harmonization.acronym >
          <div>
            <span>{{harmonization.acronym}}</span>
          </div>
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(variable) in variables" :key=variable.name >
        <th class="text-nowrap" scope="row">{{variable.name}}</th>
        <td v-for="(harmonization) in harmonizations" :key=harmonization.acronym >#</td>
      </tr>
    </tbody>
  </table>
</div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'

export default {
  name: "LifeCycleHarmonizationView",
  computed: {
    ...mapGetters(['harmonizations', 'variables']),
  },
  methods: {
    ...mapActions(['fetchHarmonizations', 'fetchMappings'])
  },
  mounted () {
    this.fetchHarmonizations()
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
