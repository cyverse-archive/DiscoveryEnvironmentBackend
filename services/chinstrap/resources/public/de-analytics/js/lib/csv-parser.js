jQuery.fn.table2CSV = function(options) {
    var options = jQuery.extend({
        separator: ',',
        header: [],
        delivery: 'popup' // popup, value
    },
    options);

    var csvData = [];
    var headerArr = [];
    var el = this;

    //header
    var numCols = options.header.length;
    var tmpRow = []; // construct header avalible array

    if (numCols > 0) {
        for (var i = 0; i < numCols; i++) {
            tmpRow[tmpRow.length] = formatData(options.header[i]);
        }
    } else {
        $(el).filter(':visible').find('th').each(function() {
            if ($(this).css('display') != 'none') tmpRow[tmpRow.length] = formatData($(this).html());
        });
    }

    row2CSV(tmpRow);

    // actual data
    $(el).find('tr').each(function() {
        var tmpRow = [];
        $(this).filter(':visible').find('td').each(function() {
            if ($(this).css('display') != 'none') tmpRow[tmpRow.length] = formatData($(this).html());
        });
        row2CSV(tmpRow);
    });
    if (options.delivery == 'popup') {
        var mydata = csvData.join('\n');
        return popup(mydata);
    } else if(options.delivery == 'download') {
        var mydata = csvData.join('\n');
        return download("csv.txt", mydata);
    } else {
        var mydata = csvData.join('\n');
        return mydata;
    }

    function row2CSV(tmpRow) {
        var tmp = tmpRow.join('') // to remove any blank rows
        // alert(tmp);
        if (tmpRow.length > 0 && tmp != '') {
            var mystr = tmpRow.join(options.separator);
            csvData[csvData.length] = mystr;
        }
    }
    function formatData(input) {
        // replace " with â€œ
        var regexp = new RegExp(/["]/g);
        var output = input.replace(regexp, "â€œ");
        //HTML
        var regexp = new RegExp(/\<[^\<]+\>/g);
        var output = output.replace(regexp, "");
        if (output == "") return '';
        return '"' + output + '"';
    }

    function popup(data) {
        var generator = window.open('', 'csv', 'height=400,width=600');
        generator.document.write('<html><head><link href="/css/style.css" rel="stylesheet" type="text/css">');
        //generator.document.write('<script src="/js/download.js" type="text/javascript"></script>');
        generator.document.write('<title>CSV</title></head><body>');
        //generator.document.write('<button style="float:left;" onclick="download("'+new Date().getTime()+' CSV.txt",document.getElementById("csv").innerHTML)">');
        //generator.document.write('Download Raw</button><br>');
        generator.document.write('<pre id="csv">');
        generator.document.write(data);
        generator.document.write('</pre>');
        generator.document.write('</body></html>');
        generator.document.close();
        return true;
    }
};
