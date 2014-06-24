var chart;
var chartData = [];

function createChart() {

    generateChartData();

    // SERIAL CHART
    chart = new AmCharts.AmSerialChart();
    chart.dataProvider = chartData;
    chart.categoryField = "date";
    chart.startDuration = 1;
    chart.balloon.adjustBorderColor = true;
    chart.balloon.fillColor = "#db6619";
    chart.balloon.borderThickness = 0;
    chart.balloon.cornerRadius = 3;

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
    categoryAxis.labelRotation = 65;
    categoryAxis.parseDates = true;
    categoryAxis.minPeriod = "MM";
    categoryAxis.gridAlpha = 0.07;
    categoryAxis.axisColor = "#CACACA";
    categoryAxis.equalSpacing = true;
    categoryAxis.dateFormats = [{period: "DD", format: "DD"},
                                {period: "MM", format: "MMM YY"},
                                {period: "YYYY", format: "MMM YY"}];

    // GRAPH
    var graph = new AmCharts.AmGraph();
    graph.valueField = "count";
    graph.balloonText = "[[category]]: [[value]]";
    graph.type = "column";
    graph.lineAlpha = 0;
    graph.lineColor = "#0098AA";
    graph.fillAlphas = 0.8;
    graph.labelText = "[[count]]";
    chart.addGraph(graph);

    //WRITE
    chart.write("chart");
};

function generateChartData() {
    chartData = [];
    var response;
    request = $.ajax({
        url: "/de-analytics/get-month-data/" + $('option:selected').attr("data"),
        async: false,
        contentType: "application/json",
        success: function(data){
            response = data;
        }
    });

    response.forEach(pushData);
    function pushData (element) {
        chartData.push({
            date: new Date(element['date']),
            count: element['count']});
    }
    $('#firstDate').html(new Date(response[0]['date']).toDateString());
}

function reloadChart(){
    $("#chart").html("");
    createChart()
}
