var template = $('#template').html();

$(function() {
	loadData();
	Mustache.parse(template);
})

function loadData(dir) {
	dir = typeof dir !== 'undefined' ? dir : '/';
	$.getJSON(getUri(dir), function(data) {
		renderPath(dir);
		$('#directory').html('');
		var rendered = Mustache.render(template, addHelpers(data));
		$('#directory').html(rendered);
	});
}

function getUri(path) {
    return window.location.pathname.split('/').splice(0, 2).join('/') + '/ls' + path;
}

function addHelpers(data) {
	data.img = function() {
		return function(type, render) {
			var mimeType = render(type);
			var file = (typeof mimeTypes[mimeType] == 'undefined' ? 'file' : mimeTypes[mimeType]);
			return '/' + appName + '/img/' + file + '.png';
		}
	}
	return data;
}

function createPathSeparator() {
	var li = document.createElement('li');
	li.innerHTML = '/';
	return li;
}

function createPathItem(path, item) {
	var li = document.createElement('li');
	li.dataset.href = path;
	li.innerHTML = item;
	li.className = 'dir';
	return li;
}

function renderPath(path) {
	$('.path').html('');
	var dirs = path.split('/');
	dirs.shift();
	$('.path').append(createPathItem('/', 'path:'));
	var partialPath = '';
	for (var dir in dirs) {
		partialPath += '/' + dirs[dir];
		$('.path').append(createPathSeparator());
		$('.path').append(createPathItem(partialPath, dirs[dir]));
	}
}

$('#directory').delegate('.tile', 'click', function() {
	var uri = $(this).attr('data-href');
	if (this.dataset.mimetype == 'inode/directory') {
		loadData(uri);
	} else {
		window.open(getUri(uri), '_blank');
	}
});

$('.path').delegate('li', 'click', function() {
	loadData($(this).attr('data-href'));
});