<template>
  <div class="mg-report-title">
    <h4><span class="badge badge-secondary">{{ type }}</span></h4>
    <h1>{{name}}</h1>
  </div>
</template>

<script>
export default {
  name: 'ReportTitle',
  props: {
    type: String,
    name: String
  }
}
</script>
