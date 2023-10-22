# ScatterPlot

## Props

<!-- @vuese:ScatterPlot:props:start -->
|Name|Description|Type|Required|Default|
|---|---|---|---|---|
|chartId|a unique ID for the chart|`String`|`true`|-|
|title|A title that describes the chart|`String`|`false`|-|
|description|Additional information to display below the title|`String`|`false`|-|
|chartData|the dataset to plot|`Array`|`true`|-|
|xvar|Name of the column that contains values to plot along the x-axis|`String`|`true`|-|
|yvar|Name of the column that contains values to plot along the y-axis|`String`|`true`|-|
|group|Name of the column that contains the groups that will be used to differeniate the points|`String`|`false`|`null`|
|xMin|Specify the min value of the x-axis. If left undefined, min value will be automatically calculated using `d3.min`|`Number`|`false`|`0`|
|xMax|Specify the max value of the x-axis. If left undefined, max value will be automatically calculated using `d3.max`|`Number`|`false`|`0`|
|yMin|Specify the min value of the y-axis. If left undefined, min value will be automatically calculated using `d3.min`|`Number`|`false`|`0`|
|yMax|Specify the max value of the y-axis. If left undefined, max value will be automatically calculated using `d3.max`|`Number`|`false`|`0`|
|xTickValues|Specify the x-axis ticks|`Array`|`false`|-|
|yTickValues|Specify the y-axis ticks|`Array`|`false`|-|
|xAxisLabel|A label that describes the x-axis|`String`|`false`|-|
|yAxisLabel|A label that describes the y-axis|`String`|`false`|-|
|pointRadius|set the radius of the points|`Number`|`false`|`5`|
|pointFill|Set the fill for all points|`String`|`false`|`#6C85B5`|
|pointFillPalette|An object containing a mapping between subcategories (`xvar`) and colors. Subcategories must match the value in the data otherwise fills colors will not be joined with the data. If you would like to change the labels, you must recode the data before passing it into this component.|`Object`|`false`|{}|
|chartHeight|set the height of the chart. Width is determined by the dimensions of the parent container so that the chart is responsive. If you would like to specify the width of the chart, use CSS or adjusted the `chartHeight`.|`Number`|`false`|`425`|
|chartMargins|adjust the chart margins|`Object`|`false`|`{ top: 15, right: 15, bottom: 60, left: 60 }`|
|enableClicks|If `true`, click events will be enabled for all columns. When a column is clicked, the row-level data for that column will be emitted. To access the data, use the event `@column-clicked = ...`|`Boolean`|`false`|`false`|
|enableTooltip|If `true`, tooltip events will be enabled for all points. When a point is hovered, the x and y values are display, as well as the group if defined|`Boolean`|`false`|`false`|
|tooltipTemplate|A function that controls the HTML content in the tooltip. The x and y values are displayed by default, as well as the group (if defined). However, you may specify the content in the body of the tooltip by defining a new function. Row-level data can be accessed by supplying `row` in the function. E.g., `(row) => { return ...}`.|`Function`|`false`|`<p>x: ${row[this.yvar]}</p><p>y: ${row[this.xvar]}</p>`|
|enableChartLegend|If `true` (default), a legend will be rendered in the below the chart only when a value is supplied to the `groups` property. Use props `stackLegend`... to customise the legend. Additional styling should be made in the css/scss file.|`Boolean`|`false`|`true`|
|stackLegend|If `true`, all legend items will be stacked (i.e., vertically arranged).|`Boolean`|`false`|false|
|enableLegendClicks|If `true`, click events will be enabled for legend groups. This allows you to toggle chart elements when a group is clicked.|`Boolean`|`false`|true|

<!-- @vuese:ScatterPlot:props:end -->


## Events

<!-- @vuese:ScatterPlot:events:start -->
|Event Name|Description|Parameters|
|---|---|---|
|point-clicked|-|-|

<!-- @vuese:ScatterPlot:events:end -->


