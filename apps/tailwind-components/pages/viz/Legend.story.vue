<script setup lang="ts">
const selections = ref<string[]>();

const legendData = {
  "Group A": "#f6eff7",
  "Group B": "#bdc9e1",
  "Group C": "#67a9cf",
  "Group D": "#02818a",
};
</script>

<template>
  <div class="[&>p]:mb-3 [&>p]:mt-1">
    <h4 class="text-heading-2xl">Legend Item Marker Component</h4>
    <p>
      The legend component has one child component:
      <code>vizLegendMarker</code>. This component allows you to change the
      symbol for each legend item. You can either render the markers as a square
      or cirle. This is useful for visually linking chart types with the
      legends. For example, if you are creating a scatter plot, it is better to
      render the legend items using the circle item rather than the square. By
      default, the marker type is a circle. The color (i.e., fill) of the marker
      can be set by using the <code>fill</code> parameter.
    </p>
    <pre class="block bg-gray-200 p-1 mb-3 whitespace-pre-line">
      {{  `<vizLegendMarker marker-type="circle" fill="orange"/>` }}
      {{  `<vizLegendMarker marker-type="square" fill="orange"/>` }}
    </pre>
    <div class="flex flex-row gap-1 p-1 my-4">
      <vizLegendMarker marker-type="circle" fill="orange" />
      <vizLegendMarker marker-type="square" fill="orange" />
    </div>
    <h4 class="text-heading-2xl">The Legend Component</h4>
    <p>
      The purpose of the <code>vizLegend</code> component is to provide context
      for charts that display grouped data. The color palette in the chart and
      the legend should be the same. The input data is an object containing one
      or more key-value pairs. The key must by the group name and the value is
      the color. For example:
    </p>
    <pre class="block bg-gray-200 p-1 mb-3 whitespace-normal">
      {{ legendData }}
    </pre>
    <p>This produces the following legend.</p>
    <vizLegend
      legend-id="default-legend"
      :data="legendData"
      :stack-legend="false"
    />
    <p>
      The legend can also be stacked using <code>:stack-legend="true"</code>
    </p>
    <pre class="block bg-gray-200 p-1 mb-3 whitespace-normal">
      {{ `<vizLegend legend-id="default-legend" :data="legendData" :stack-legend="true"/>` }}
    </pre>
    <vizLegend
      legend-id="default-legend"
      :data="legendData"
      :stack-legend="true"
    />
    <p>
      Interactive features are also available on the legend component. These
      features can be used to control a chart by extracting the value of a
      legend item (i.e., the label). You can enable/disable hovering and clicks
      in a legend.
    </p>
  </div>
</template>
