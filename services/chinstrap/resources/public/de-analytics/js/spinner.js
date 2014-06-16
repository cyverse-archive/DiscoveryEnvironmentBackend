$(document).ready(function(){
    var opts = {
        lines: 13, // The number of lines to draw
        length: 7, // The length of each line
        width: 4, // The line thickness
        radius: 10, // The radius of the inner circle
        rotate: 0, // The rotation offset
        color: '#000', // #rgb or #rrggbb
        speed: 1, // Rounds per second
        trail: 60, // Afterglow percentage
        shadow: false, // Whether to render a shadow
        hwaccel: true, // Whether to use hardware acceleration
        top: '0px', // Top position relative to parent in px
        left: 'auto' // Left position relative to parent in px
    };

    var target = document.getElementById('loader');
    var spinner = new Spinner(opts).spin(target);
});
