# ChartLegend

Create a legend for a visualisation for use outside the chart element. This component may be useful if you have several charts that display the same groups. 

## Props

<!-- @vuese:ChartLegend:props:start -->
|Name|Description|Type|Required|Default|
|---|---|---|---|---|
|legendId|a unique ID for the legend|`String`|`true`|-|
|data|One or more key-value pairs|`Object`|`true`|-|
|stackLegend|If true (default), all legend items will be stacked|`Boolean`|`false`|true|
|enableClicks|If `true`, click events will be enabled for all labels. When a label is clicked, the row-level data for that label will be emitted. To access the data, use the event `@legend-item-clicked=(value) => ...`|`Boolean`|`false`|`false`|
|enableHovering|If `true`, mouseover event will be enabled for all labels. When a label is clicked, the row-level data for that item will be emitted. To access the data, use the event `legend-item-hovered=(value)=>...`|`Boolean`|`false`|`false`|

<!-- @vuese:ChartLegend:props:end -->


## Events

<!-- @vuese:ChartLegend:events:start -->
|Event Name|Description|Parameters|
|---|---|---|
|legend-item-clicked|-|-|
|legend-item-mouseover|-|-|
|legend-item-mouseout|-|-|

<!-- @vuese:ChartLegend:events:end -->


