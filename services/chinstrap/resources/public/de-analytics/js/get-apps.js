document.addEventListener("DOMContentLoaded", function(){
    getApps()
    window.setInterval(getApps,36000)
})

function getApps() {
    $.get("/de-analytics/get-apps", function(resp) {

        $('#running').html(resp['running'])
        $('#submitted').html(resp['submitted'])
        $('#failed').html(resp['failed'])
        $('#completed').html(resp['completed'])

        if(resp['running'] === 0)
            $('#running-apps').html('There are currently no running apps.')
        else
            $('#running-apps').html(resp['running-names'])

        if(resp['submitted'] === 0)
            $('#submitted-apps').html('There are currently no submitted apps.')
        else
            $('#submitted-apps').html(resp['submitted-names'])

        if(resp['failed'] === 0)
            $('#failed-apps').html('There are currently no failed apps. Woohoo!')
        else
            $('#failed-apps').html(resp['failed-names'])
    })
}
