# BarChart

Create a bar chart (horizontal bars) that displays values along the x-axis by groups (along the y-axis). This component is ideal if you have many categorical variables or you would like to display ordinal data. However, if you many groups, consider combining or excluding groups with smaller values. If the labels excede the chart bounds, abbreviate labels and adjust the chart margins to allow for more space around the chart area. You may also want to consider describing the abbreviations in a figure caption or elsewhere on the page.

## Props

<!-- @vuese:BarChart:props:start -->

| Name            | Description                                                                                                                                                                                                             | Type      | Required | Default                                        |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | -------- | ---------------------------------------------- |
| chartId         | a unique ID for the chart                                                                                                                                                                                               | `String`  | `true`   | -                                              |
| title           | A title that describes the chart                                                                                                                                                                                        | `String`  | `false`  | -                                              |
| description     | Additional information to display below the title                                                                                                                                                                       | `String`  | `false`  | -                                              |
| xvar            | Name of the column that contains the values to plot along the x-axis                                                                                                                                                    | `String`  | `true`   | -                                              |
| yvar            | Name of the column that contains the groups to plot along the y-axis                                                                                                                                                    | `String`  | `true`   | -                                              |
| xMax            | Specify the max value of the x-axis. If left undefined, max value will be automatically calculated using `d3.max`                                                                                                       | `Number`  | `false`  | -                                              |
| xTickValues     | Specify the x-axis ticks                                                                                                                                                                                                | `Array`   | `false`  | -                                              |
| xAxisLabel      | A label that describes the x-axis                                                                                                                                                                                       | `String`  | `false`  | -                                              |
| yAxisLabel      | A label that describes the y-axis                                                                                                                                                                                       | `String`  | `false`  | -                                              |
| chartData       | the dataset the plot                                                                                                                                                                                                    | `Array`   | `true`   | -                                              |
| chartHeight     | set the height of the chart. Width is determined by the dimensions of the parent container so that the chart is responsive. If you would like to specify the width of the chart, use CSS or adjusted the `chartHeight`. | `Number`  | `false`  | `425`                                          |
| chartMargins    | adjust the chart margins                                                                                                                                                                                                | `Object`  | `false`  | `{ top: 15, right: 15, bottom: 60, left: 65 }` |
| barFill         | Set the fill of all bars (hex code)                                                                                                                                                                                     | `String`  | `false`  | `#6C85B5`                                      |
| barHoverFill    | Set the color that is displayed when a bar is hovered (hex code)                                                                                                                                                        | `String`  | `false`  | `#163D89`                                      |
| barPaddingInner | Adjust the amount of blank space inbetween bar between 0 and 1                                                                                                                                                          | `Number`  | `false`  | `0.2`                                          |
| barPaddingOuter | Adjust the amount of blank space before the first bar and after the last bar. Value must be between 0 and 1.                                                                                                            | `Number`  | `false`  | `0.2`                                          |
| barAlign        | Along with `barPaddingOuter`, specify how the bars are distributed y-axis. A value of 0 will position the bars closer to the x-axis.                                                                                    | `Number`  | `false`  | `0.5`                                          |
| enableClicks    | If `true`, click events will be enabled for all bars. When a bar is clicked, the row-level data for that bar will be emitted. To access the data, use the event `@bar-clicked = ...`                                    | `Boolean` | `false`  | `false`                                        |
| enableAnimation | If `true`, bars will be drawn over 500ms from the y-axis.                                                                                                                                                               | `Boolean` | `false`  | true                                           |

<!-- @vuese:BarChart:props:end -->

## Events

<!-- @vuese:BarChart:events:start -->

| Event Name  | Description | Parameters |
| ----------- | ----------- | ---------- |
| bar-clicked | -           | -          |

<!-- @vuese:BarChart:events:end -->
