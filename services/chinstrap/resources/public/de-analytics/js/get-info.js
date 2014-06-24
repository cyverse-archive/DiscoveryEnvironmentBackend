function getInfo() {
    date = $.datepicker.formatDate('@', $('#date').datepicker('getDate'));
    $.get("/de-analytics/get-info/" + date, function(resp) {
        if(resp['tools'] == "")
            $('#inner').html( "No apps executed on " + $('#date').val() + ".");
        else {
            tools = "Tools on " + $('#date').val() + ":<hr>";

            tools += "<table id='app-info'><thead>" +
                "<tr><th>Name</th><th>Count</th></tr>" +
                "</thead><tbody>";

            for(var i = 0; i < resp['tools'].length; i++){
                tools +=
                    "<tr><td>" +
                    resp['tools'][i]['name'] +
                    "</td><td>" +
                    resp['tools'][i]['count'] +
                    "</td></tr>";
            }

            tools +=
                "</tbody></table><br>" +
                "<button class='left'" +
                "onclick=\"$('#app-info').table2CSV(" +
                "{header:['Apps On: "+ $('#date').val() +"','Count']})\">" +
                "Export to CVS</button>";

            $('#inner').html(tools);
        }
    })
};
