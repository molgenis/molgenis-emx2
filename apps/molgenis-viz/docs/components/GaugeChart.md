# GaugeChart

## Props

<!-- @vuese:GaugeChart:props:start -->

| Name         | Description                                                                                                                                                                                                             | Type      | Required | Default            |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | -------- | ------------------ |
| chartId      | A unique ID for the chart                                                                                                                                                                                               | `String`  | `true`   | -                  |
| title        | A title that describes the chart                                                                                                                                                                                        | `String`  | `false`  | -                  |
| description  | Additional information to display below the title                                                                                                                                                                       | `String`  | `false`  | -                  |
| value        | A decimal between 0 and 1                                                                                                                                                                                               | `Number`  | `true`   | -                  |
| valueFill    | Set the color of the foreground layer (i.e., value)                                                                                                                                                                     | `String`  | `false`  | `#1c9099`          |
| baseFill     | Set the color of the background layer                                                                                                                                                                                   | `String`  | `false`  | default: '#c7eae5' |
| chartHeight  | set the height of the chart. Width is determined by the dimensions of the parent container so that the chart is responsive. If you would like to specify the width of the chart, use CSS or adjusted the `chartHeight`. | `Number`  | `false`  | `300`              |
| chartMargins | set all chart margins                                                                                                                                                                                                   | `Number`  | `false`  | `10`               |
| chartScale   | set chart scale                                                                                                                                                                                                         | `Number`  | `false`  | `0.75`             |
| enableClicks | If `true`, click events will be enabled for all bars. When a bar is clicked, the row-level data for that bar will be emitted. To access the data, use the event `@slice-clicked=...`                                    | `Boolean` | `false`  | `false`            |

<!-- @vuese:GaugeChart:props:end -->

## Events

<!-- @vuese:GaugeChart:events:start -->

| Event Name  | Description | Parameters |
| ----------- | ----------- | ---------- |
| arc-clicked | -           | -          |

<!-- @vuese:GaugeChart:events:end -->
