# ProgressMeter

Create a progress meter.

## Props

<!-- @vuese:ProgressMeter:props:start -->

| Name            | Description                                               | Type      | Required | Default   |
| --------------- | --------------------------------------------------------- | --------- | -------- | --------- |
| chartId         | A unique ID for the chart                                 | `String`  | `true`   | -         |
| title           | A title that describes the chart                          | `String`  | `true`   | -         |
| value           | value that indicates how much progress has been made      | `Number`  | `false`  | -         |
| totalValue      | the total possible value                                  | `Number`  | `false`  | -         |
| barHeight       | Set the height of all bar                                 | `Number`  | `false`  | `35`      |
| barFill         | Set the fill of all bars (hex code)                       | `String`  | `false`  | `#6C85B5` |
| enableAnimation | If `true`, bars will be drawn over 500ms from the y-axis. | `Boolean` | `false`  | true      |

<!-- @vuese:ProgressMeter:props:end -->
