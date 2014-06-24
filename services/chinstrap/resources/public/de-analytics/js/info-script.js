$(document).ready(function(){
    // i keeps track of the picked date relation to today.
    var i = 0;

    // pre-init datepicker options
    var picker = {showOn:'both',
                  hideIfNoPrevNext: true,
                  maxDate: '+0d',
                  buttonText:'Pick a Date'};

    //init date picker
    $('#date').datepicker(picker);

    //on page load default to showing today's data.
    $('#date').datepicker('setDate', 'today');
    getInfo();

    // Convenience Keybindings - Using mousetrap.js
    // directional keys increment/decrment through days
    Mousetrap.bind(['down', 'left', 'j', 'h'], function() {
        i--;
        $('#date').datepicker('setDate', i);
        getInfo();
    });

    Mousetrap.bind(['up', 'right', 'k', 'l'], function() {
        i++;
        if(i < 0){
            $('#date').datepicker('setDate', i);
        } else {
            $('#date').datepicker('setDate', 'today');
            i = 0;
        }
        getInfo();
    });

    // t key quickly resets the date to today
    Mousetrap.bind(['t'], function() {
        i = 0;
        $('#date').datepicker('setDate', 'today');
        getInfo();
    });

    // for fun
    Mousetrap.bind('r a m o n e s', function() {
            $('#inner').html("Hey Ho, Lets Go!");
    });

    Mousetrap.bind('c h i n s t r a p', function() {
            $('#inner').html("The Chinstrap has you.");
    });
});
