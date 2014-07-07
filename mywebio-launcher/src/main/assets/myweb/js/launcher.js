$(function() {
    $.getJSON(getUri('/apps'), function(data) {
        var template = $('#template').html();
        Mustache.parse(template);
        var rendered = Mustache.render(template, data);
        $('#target').html(rendered);
    });
})

function getUri(path) {
    return window.location.pathname.split('/').splice(0, 2).join('/') + path;
}

$('body').delegate('img', 'mouseover', function() {
//    $(this).animate({opacity: 1, width: '+=20', height: '+=20'}, 1000);
    $(this).fadeTo('fast', 1);
}).delegate('img', 'mouseout', function() {
    $(this).fadeTo('fast', 0.6);
});
