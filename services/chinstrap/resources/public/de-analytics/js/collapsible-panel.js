(function($) {
    $.fn.extend({
        collapsiblePanel: function() {
            // Call the ConfigureCollapsiblePanel function for the selected element
            return $(this).each(ConfigureCollapsiblePanel);
        }
    });

})(jQuery);

var i = 0;

function ConfigureCollapsiblePanel() {
    $(this).addClass("ui-widget");

    // Wrap the contents of the container within a new div.
    $(this).children().wrapAll("<div class='collapsibleContainerContent ui-widget-content'></div>");

    // Create a new div as the first item within the container.  Put the title of the panel in here.
    $("<div class='collapsibleContainerTitle ui-widget-header'><div>" + $(this).attr("title") + "<img class='arrow' src='/de-analytics/img/circle_arrow_right.png'></img></div></div>").prependTo($(this));

    // Assign a call to CollapsibleContainerTitleOnClick for the click event of the new title div.
    $(".collapsibleContainerTitle", this).click(CollapsibleContainerTitleOnClick);
}

function CollapsibleContainerTitleOnClick(e) {
    // The item clicked is the title div... get this parent (the overall container) and toggle the content within it.
    var $el = $(e.currentTarget);
    if ($el.hasClass('expanded')) {
        $el.find('.arrow').attr("src" , "/de-analytics/img/circle_arrow_right.png");
        $el.removeClass('expanded');
    } else {
        $el.find('.arrow').attr("src" , "/de-analytics/img/circle_arrow_down.png");
        $el.addClass('expanded');
    }
    $(".collapsibleContainerContent", $(this).parent()).slideToggle();
    i++;
}
