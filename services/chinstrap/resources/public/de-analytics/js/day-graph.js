var chart;
var chartData = [];
var chartCursor;

function createChart(){

    generateChartData();

    // SERIAL CHART
    chart = new AmCharts.AmSerialChart();
    chart.pathToImages = "/de-analytics/img/";
    chart.zoomOutButton = {
        backgroundColor: '#000000',
        backgroundAlpha: 0.15
    };
    chart.dataProvider = chartData;
    chart.categoryField = "date";
    chart.balloon.bulletSize = 5;

    // listen for "dataUpdated" event (fired when chart is rendered) and call zoomChart method when it happens
    chart.addListener("dataUpdated", zoomChart);
    // while loading hack
    var init = false;
    chart.addListener("zoomed", function (event) {
        if(!init) {
            $('#loader').hide();
            init = true;
        }
    });

    // AXES
    // category
    var categoryAxis = chart.categoryAxis;
    categoryAxis.parseDates = true; // as our data is date-based, we set parseDates to true
    categoryAxis.minPeriod = "DD"; // our data is daily, so we set minPeriod to DD
    categoryAxis.dashLength = 1;
    categoryAxis.gridAlpha = 0.15;
    categoryAxis.autoGridCount = false;
    categoryAxis.gridCount = 10;
    categoryAxis.position = "top";
    categoryAxis.axisColor = "#CACACA";
    categoryAxis.dateFormats = [{
            period: "DD",
            format: "DD"
        }, {
            period: "WW",
            format: "MMM DD"
        }, {
            period: "MM",
            format: "MMM"
        }, {
            period: "YYYY",
            format: "YYYY"
        }];

    // value
    var valueAxis = new AmCharts.ValueAxis();
    valueAxis.axisAlpha = 0.15;
    valueAxis.title = "# of Apps";
    valueAxis.dashLength = 1;
    chart.addValueAxis(valueAxis);

    // GRAPH
    var graph = new AmCharts.AmGraph();
    graph.title = "Apps Ran Over Time";
    graph.labelText = "[[count]]";
    graph.valueField = "count";
    graph.bullet = "round";
    graph.bulletBorderColor = "#FFF";
    graph.bulletBorderThickness = 2;
    graph.lineThickness = 2;
    graph.lineColor = "#0098AA";
    graph.negativeLineColor = "#AADDCC";
    graph.hideBulletsCount = 100;
    chart.addGraph(graph);

    // CURSOR
    chartCursor = new AmCharts.ChartCursor();
    chartCursor.cursorColor = '#db6619';
    chartCursor.categoryBalloonColor = '#db6619';
    chartCursor.cursorPosition = "mouse";
    chartCursor.pan = true; // set it to false if you want the cursor to work in "select" mode
    chart.addChartCursor(chartCursor);

    // SCROLLBAR
    var chartScrollbar = new AmCharts.ChartScrollbar();
    chartScrollbar.graph = graph;
    chartScrollbar.autoGridCount = true;
    chartScrollbar.scrollbarHeight = 25;
    chart.addChartScrollbar(chartScrollbar);

    // WRITE
    chart.write("chart");
};

function generateChartData() {
    chartData = [];
    var response;
    request = $.ajax({
        url: "/de-analytics/get-day-data/" + $('option:selected').attr("data"),
        async: false,
        contentType: "application/json",
        success: function(data){
            response = data;
        }
    });

    response.forEach(formatDate);

    function formatDate (element) {
        var d = new Date(element['date'] * 1).toDateString();
        d = new Date(d);
        element['date'] = d;
    }

    var firstDate = response[0]['date'];

    $("#firstDate").html("" + firstDate);
    $("#day").addClass("active");

    var daysBetween = Math.round(Math.abs(firstDate - new Date().getTime())/8640000);

    for(var i = 0; i <= daysBetween; i++) {
        var newDate = new Date(firstDate);
        newDate.setDate(newDate.getDate() + i);
        for(var j = 0; j < response.length; j++){
            if(response[j]['date'].getTime() == newDate.getTime()){
                chartData.push({
                    date:  response[j]['date'],
                    count: response[j]['count']
                });
                response = _.rest(response);
                break;
            } else {
                chartData.push({
                    date:  newDate,
                    count: 0
                });
            }
        }
    }
}

// this method is called when chart is first inited as we listen for "dataUpdated" event
function zoomChart() {
    // different zoom methods can be used - zoomToIndexes, zoomToDates, zoomToCategoryValues
    chart.zoomToIndexes(chartData.length - 40, chartData.length - 1);
}

function reloadChart(){
    $("#chart").html("")
    createChart()
}

// changes cursor mode from pan to select
function setPanSelect() {
    if (document.getElementById("rb1").checked) {
        chartCursor.pan = false;
        chartCursor.zoomable = true;
    } else {
        chartCursor.pan = true;
    }
    chart.validateNow();
}
