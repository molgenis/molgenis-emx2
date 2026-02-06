# Pie Charts

Pie charts are commonly used to show _parts of a whole_ where each segment (or slice) represents are percentage of a category.

## Best practices

It is recommended to minimise the number of values that are displayed. Ideally, no more than 5 values should be provided. Otherwise, there is a risk of overlapping labels due to smaller segments and a finite container. If your dataset contains many values, consider presenting this information as a table or as a column chart.

It is expected that sum of the values will always equal 100%.

If the data contains values that are less than 10%, then combine these values into an "other" category. This will reduce the risk of overlapping labels and the need to generate chart elements that have smaller values. If you need to display all values, consider using another chart type (e.g., column chart, table).

Titles and descriptions are useful for providing context for users. It is recommended to define these for good web accessibility practices. These should be short and give enough context on what is displayed in the chart.

## Properties

| Name                        | Description                                                 | Type     | Required | Default Value |
|:----------------------------|:------------------------------------------------------------|:---------|:---------|:--------------|
| id                          | a unique identifier for the chart                           | `string` | `true`   | -             |
| title                       | chart title                                                 | `string` | `true`   | -             |
| description                 | additional context for a chart                              | `string` | -        | -             |
| width                       | change the width of the chart                               | `number` | -        | `300`         |
| height                      | change the height of the chart                              | `number` | -        | `300`         |
| margins                     | change the spacing around the chart                         | `number` | -        | `25`          |
| data                        | data used to generate the pie segments                      | `object` | `true`   | -             |
| colorPalette                | a custom color scheme                                       | `object` | -        | `SchemeBlues` |
| showValues                  | If true, values will be displayed                           | `bool`   | -        | `true`        |
| showLabels                  | If true, labels will be displayed                           | `bool`   | -        | `true`        |
| showValuesAsPercentages     | If true, a `%` will be added to each value                  | `bool`   | -        | `false`       |
| asDonutChart                | If true, chart will be rendered as a donut chart            | `bool`   | -        | `false`       |
| hoverEventsAreEnabled       | Enable hover events on segments                             | `bool`   | _        | `true`        |
| clickEventsAreEnabled       | Enable events when a segment is clicked                     | `bool`   | _        | `false`       |
| legendIsEnabled             | Display or hide the chart legend                            | `bool`   | _        | `true`        |
| legendIsStacked             | If true, the legend is arrange vertically                   | `bool`   | _        | `false`       |
| legendPosition              | Placement of the legend: `top` or `bottom`                  | `bool`   | _        | `"top`        |
| legendHoverEventsAreEnabled | Allow legend hover events to apply hover events on segments | `bool`   | _        | `false`       |

## Chart Interactivity and events

| Name                   | Description                                                                                                |
|------------------------|------------------------------------------------------------------------------------------------------------|
| `@slice-clicked`       | If prop `click-events-are-enabled` is true, then the value of a segment will be emitted when it is clicked |
| `@legend-item-clicked` | TBD                                                                                                        |

### Segment click events

If enabled, when a user clicks a segment, the value will be emitted. This is useful if you would like to pass that value into other chart, use it in a query, or alter the UI in any way. This feature is only possible if `click-events-are-enabled` is set to true. The emitted value is an object containing the label and value of the clicked segment.

```vue
<PieChart :click-events-are-enabled="true" @slice-clicked="console.log($event)" ... />
```

## Example

Data must be an object that contains one or more key-value pairs. The follow example shows how to transform the data into the desired format. It is expected that data transformations will be made outside of this component.

```ts
// original data
const data = [
    { group: "A", value: 62 },
    { group: "B", value: 18 },
]

// prepare data by reshaping data to group-percent pairs
const total = data.reduce((acc, d) => acc + d.value, 0)
const chartData = Object.fromEntries(data.map((d) => {
    const percent = (d.value / total);
    return [d.group, percent]
}));

// > Object { A: 0.775, B: 0.225 }
```

```vue
<PieChart
    id="group-assignment-chart"
    title="Allocation of research participants"
    description="Participants (N=80) were randomised into Group A (n=62) or Group B (n=18)"
    :data="chartData"
    :show-values-as-percentages="true"
/>
```
