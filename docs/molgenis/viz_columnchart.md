# Column Charts

Column charts are useful for displaying values for one or more groups where the height of columns indicates a _higher_ value.

If you would like to group data by an additional level, use the `GroupedColumnChart` component.

## Best practices

To ensure good data visualisation, please follow these recommendations.

1. __Reduce the number of categories plotted along the x-axis__: This prevents cluttered charts, overlapping elements, and other issues that affect readability. It is recommended to display 5-7 categories. If you need to display more items, consider grouping data, creating multiple charts or display data as a table.
2. __Do not apply colours to every column__: Colour coding every column may _look nice_ but it makes charts difficult to read and results in poor accessibility. Instead, use a single colour for all columns and adjust the hover colour. In some cases, highlighting a specific value may be useful (e.g., significant value, unexpected result, etc.). If this is the case, create a palette that contains a colour that sticks out for the column of interest and use a neutral colour for the remaining columns.
3. __Use the `BarChart` component for ordinal data__: ordinal data is best displayed in a bar chart (i.e., horizontal _bars_ as opposed to vertical) where data is sorted in descending order by value.

## Properties

| Name                  | Description                                             | Type       | Required | Default Value |
|:----------------------|:--------------------------------------------------------|:-----------|:---------|:--------------|
| id                    | a unique identifier for the chart                       | `string`   | `true`   | -             |
| title                 | chart title                                             | `string`   | `true`   | -             |
| description           | additional context for a chart                          | `string`   | -        | -             |
| width                 | change the width of the chart                           | `number`   | -        | `300`         |
| height                | change the height of the chart                          | `number`   | -        | `300`         |
| data                  | data used to render the columns                         | `array`    | `true`   | -             |
| xvar                  | the name of the column to plot along the x axis         | `string`   | `true`   | -             |
| yvar                  | the name of the column to plot along the y axis         | `string`   | `true`   | -             |
| ymax                  | the max value to show on the y-axis                     | `number`   | -        | -             |
| yTickValues           | an array of y-axis breakpoints                          | `number[]` | -        | _             |
| xAxisLabel            | A title to display on the x-axis                        | `string`   | -        | -             |
| yAxisLabel            | A title to display on the y-axis                        | `string`   | -        | -             |
| breakXAxisLabelsAt    | A character(s) to break long x-axis labels on new lines | `string`   | -        | -             |
| columnColor           | the color that will be applied to all columns           | `string`   | -        | `#014f9e`     |
| columnColorOnHover    | a color to apply when a column is hovered               | `string`   | -        | `#53a9ff`     |
| columnBorderColor     | color for the border around each column                 | `string`   | -        | -             |
| colorPalette          | a custom color palette                                  | `object`   | -        | -             |
| marginTop             | amount of space to add above the chart                  | `number`   | -        | -             |
| marginRight           | amount of space to add on the right side of the chart   | `number`   | -        | `10`          |
| marginBottom          | amount of space to add below the chart                  | `number`   | -        | `60`          |
| marginLeft            | Amount of space to add on the left side of the chart    | `number`   | -        | `60` /`25`    |
| enableGridlines       | If true, gridlines will be shown                        | `bool`     | -        | `false`       |
| hoverEventsAreEnabled | Enable hover events on segments                         | `bool`     | _        | `true`        |
| clickEventsAreEnabled | Enable events when a segment is clicked                 | `bool`     | _        | `false`       |
| animationsAreEnabled  | If true, columns will be drawn over 300ms               | `bool`     | -        | `true`        |

## Chart Interactivity and events

| Name              | Description                                                                                     |
|-------------------|-------------------------------------------------------------------------------------------------|
| `@column-clicked` | If prop `click-events-are-enabled` is true, then the data of the clicked column will be emitted |

### Column click example

```vue
<ColumnChart :click-events-are-enabled="true" @column-clicked="console.log($event)" ... />
```

## Example

Data must be an array of objects where each object is a row in a dataset. If the input dataset is retrieved via an API, use a `v-if` to make sure the data is available to render the chart.

```vue
<script lang="ts" setup>
import ColumnChart from "...";

const data = [
  { name: "Group A", value: 42 },
  { name: "Group B", value: 31 },
  { name: "Group C", value: 82 },
  { name: "Group D", value: 3 },
];
</script>

<template>
    <ColumnChart
        id="column-chart-demo"
        title="Participants by experimental group"
        description="Participants (n=158) were randomised into one of four groups"
        :data="data"
        xvar="name"
        yvar="value"
        x-axis-label="Experimental group"
        y-axis-label="Number of participants"
        :click-events-are-enabled="true"
        :hover-events-are-enabled="true"
        @column-clicked="selection = $event"
    />
</template>
```
