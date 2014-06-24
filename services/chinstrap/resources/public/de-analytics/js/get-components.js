document.addEventListener("DOMContentLoaded",function(){
    getComponents();
    window.setInterval(getComponents,36000);
});

function getComponents() {
    $.get("/de-analytics/get-components", function(resp) {
    $('#all').html(resp['all'])
    $('#without').html(resp['without'])
    $('#with').html(resp['with'])
    });
};
