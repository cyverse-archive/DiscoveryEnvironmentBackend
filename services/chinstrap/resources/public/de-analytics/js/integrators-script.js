var selected;
var table;
$(document).ready(function() {

    if($('#choose').val != "General Data")
        getIntegrator($('#choose').val());
    else
        generalData();

    $('.integrator').hover(
    function() {
        $(this).children('.name').text(
            $(this).children('.email').attr('value'));
        $(this).children('.name').animate({ color: '#555' }, 100)
    },
    function() {
        $(this).children('.name').text(
            $(this).children('.name').attr('value'));
        $(this).children('.name').animate({ color: '#6D929B' }, 100);
    })

    $('.integrator').click(function() {
        getIntegrator($(this).children('.id').attr('value'));
        selected = $('option:first-child').attr('selected', true);
    });

    $('#choose').change(function() {
        getIntegrator($('#choose').val());
    });

});

function generalData() {
    $.get('/de-analytics/get-integrator-data/', function(resp){
        body =
            "<h3><strong>General Integrator Info</strong></h3><hr>"
            + "<ul>"
                + "<li>In <strong>total</strong>"
                + ", integrators have contributed "
                + "<strong>" + resp['total'] + "</strong>"
                + " applications.</li>"
                + "<li>On <strong>average</strong>"
                + ", integrators create about "
                + "<strong>" + resp['average'].toPrecision(2)
                + "</strong>"
                + " applications.</li>"
            +"</ul>";
        $('#inner').html(body);
    })
};

Mousetrap.bind(['down', 'j'], function() {
    selected = $('option:selected').next('option');
    if (selected.val()) {
        selected.attr('selected', true);
        getIntegrator(selected.val());
    }else
        selected = $('option:selected').prev('option');
});

Mousetrap.bind(['up', 'k'], function() {
    selected = $('option:selected').prev('option');
    if (selected.val()) {
        selected.attr('selected', true);
        getIntegrator(selected.val());
    }else
        selected = $('option:selected').next('option');
});

Mousetrap.bind(['right', 'l'], function() {
    if(table) table.fnPageChange('next');
});

Mousetrap.bind(['left', 'h'], function() {
    if(table) table.fnPageChange('previous');
});

Mousetrap.bind(['g'], function() {
    generalData();
    selected = $('option:first-child').attr('selected', true);
});
