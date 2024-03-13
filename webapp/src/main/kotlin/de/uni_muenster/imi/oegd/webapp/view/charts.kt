package de.uni_muenster.imi.oegd.webapp.view

import kotlinx.html.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random


fun FlowContent.drawBarChart(label: String, data: Map<String, String>) = drawChart("bar", label, data)
fun FlowContent.drawPieChart(label: String, data: Map<String, String>) = drawChart("pie", label, data)

fun FlowContent.drawChart(type: String, label: String, data: Map<String, String>) {
    val backgroundColors = listOf(
        "rgba(54, 162, 235, 0.2)",
        "rgba(255, 206, 86, 0.2)",
        "rgba(75, 192, 192, 0.2)",
        "rgba(153, 102, 255, 0.2)",
        "rgba(255, 159, 64, 0.2)",
        "rgba(255, 99, 132, 0.2)"
    )
    val borderColors = listOf(
        "rgba(54, 162, 235, 1)",
        "rgba(255, 206, 86, 1)",
        "rgba(75, 192, 192, 1)",
        "rgba(153, 102, 255, 1)",
        "rgba(255, 159, 64, 1)",
        "rgba(255, 99, 132, 1)"
    )
    val randomId = "myChart" + Random.nextInt(100000)
    canvas {
        id = randomId
        width = "100%"
        height = "100%"
    }
    val labels = Json.encodeToString(data.keys)
    val dataValues = Json.encodeToString(data.values.map { it.toInt() })
    script(type = "text/javascript") {
        unsafe {
            +"""
                Chart.plugins.register({
                    beforeDraw: function(chartInstance) {
                    var ctx = chartInstance.chart.ctx;
                    ctx.fillStyle = "white";
                    ctx.fillRect(0, 0, chartInstance.chart.width, chartInstance.chart.height);
                  }
                });
            """
            +"""
                new Chart(document.getElementById('$randomId').getContext('2d'), {
                    type: '$type',
                    data: {
                        labels: $labels,
                        datasets: [{
                            label: "$label",
                            data: $dataValues,
                            backgroundColor: ${
                                if (type == "pie") Json.encodeToString(backgroundColors) else Json.encodeToString(backgroundColors[0])
                            },
                            borderColor: ${
                                if (type == "pie") Json.encodeToString(borderColors) else Json.encodeToString(backgroundColors[0])
                            },
                            borderWidth: 1,
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            yAxes: [{ 
                                ticks:{
                                    beginAtZero: true, 
                                    min: 0, 
                                    //disable non-integer ticks
                                    callback:function(val, idx){if (Number.isInteger(val)) { return val; }}
                            } }]
                        }
                    }
                });
            """
        }
    }


}
