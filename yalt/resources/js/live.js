var get_number_of_points = function() {
    return $("#num-points").val();
};

var get_secret = function() {
    // first try for the one on the page:
    var secret = $('input[name=theSecret]').val();
    if (secret == '' || secret == $('input[name=theSecret]').attr('title')) {
	// try for the hidden input:
	secret = $('input[name=secret]').val();
	if (secret == $('input[name=secret]').attr('title')) {
	    $("#secret-change-area").slideDown();
	    return 'none';		// they haven't entered a secred yet...
	}
    }
    return secret;
};

var renew_map = function() {
    clear_map();

    var secret = get_secret();

    // update the spot where the secret is displayed:
    $("#current-map-for").html(secret);

    // get the data and update the map
    var data_url = '/get?secret='+secret+'&n='+get_number_of_points();
    $.getJSON(data_url, function(obj, retval, xmlhttprequest) {
	draw_new_points(obj.data.locations);
	recenter_map();
    });
};

var draw_new_points = function(data) {
    var the_coords = [];
    var the_markers = [];
    var new_data_points = [];
    for (i in data) {
	var ll = new google.maps.LatLng(data[i].latitude,
					data[i].longitude);
	var title_text = data[i]['date'] + 'UTC' +
	    "\nLatitude: " + data[i]['latitude'] +
	    "\nLongitude: " + data[i]['longitude'] +
	    "\nAccuracy: " + data[i]['accuracy'] +
	    "\nSpeed: " + data[i]['speed'];
	// var mrkr = new google.maps.Marker({
	// 	position: ll,
	// 	title: title_text,
	// 	map: the_map
	// });
	var mrkr = new google.maps.Circle({
	    center: ll,
	    // radius: data[i]['accuracy'],
	    radius: 12,
	    fillColor: "#DD3333",
	    fillOpacity: 0.5,
	    strokeOpacity: 0,
	    map: the_map
	});

	the_coords.push(ll);
	new_data_points.push({'raw':data[i],'ll':ll});
	the_markers.push(mrkr);
    } // eo for i in locations
    new_data_points.reverse();
    data_points = data_points.concat(new_data_points);

    // just in case we only added one point:
    if (new_data_points.length == 1 && data_points.length) {
	the_coords = [
	    data_points[data_points.length-2]['ll'],
	    data_points[data_points.length-1]['ll']
	];
    }

    var the_path = new google.maps.Polyline({
	path: the_coords,
	strokeColor: "#0011AA",
	strokeOpacity: 0.7,
	strokeWeight: 5
    });

    current_overlays.push(the_path);
    current_overlays = current_overlays.concat(the_markers);


    the_path.setMap(the_map);
};


var add_new_points = function() {
    var data = {
	secret:get_secret(),
	n:get_number_of_points(),
	last:data_points[data_points.length-1]['raw'].date
    };
    var data_url = '/get?' + $.param(data);
    $.getJSON(data_url, function(obj, retval, xmlhttprequest) {
	var data = obj.data.locations.filter(function(o) {
	    for (i in data_points) {
		if (o.id == data_points[i]['raw'].id) return false;
	    }
	    return true;
	});
	draw_new_points(data);
	recenter_map();
    });
};

var recenter_map = function() {
    if (data_points.length) {
	the_map.panTo(data_points[data_points.length-1]['ll']);
    }
};

var clear_map = function() {
    for (i in current_overlays) {
	current_overlays[i].setMap(null);
	current_overlays[i];
    }
    current_overlays = [];
    data_points = [];
};

var toggle_auto_refresh = function() {
    if (refreshing_task_is_running) {
	refreshing_task_is_running = false;
	clearInterval(refreshing_task_id);
	$(this).children('.ui-button-text').html('Enable Autorefresh');
    } else {
	refreshing_task_is_running = true;
	refreshing_task_id = setInterval(add_new_points, refreshing_task_period);
	$(this).children('.ui-button-text').html('Disable Autorefresh');
    }
};


// data_points variable: all the data. will maintain order with oldest
// item in slot 0, newest at the back
var data_points = [];
var current_overlays = [];
var the_map = null;

var refreshing_task_is_running = false;
var refreshing_task_id = null;
var refreshing_task_period = 5000; // ms

$(function() {
    the_map = new google.maps.Map($("#map-canvas")[0], {
	zoom: 17,
	center: new google.maps.LatLng(33.989135,-117.339123),
	mapTypeId: google.maps.MapTypeId.HYBRID
    });

    $("#change-secret-go").button().click(function() {
	renew_map();
	$("#secret-change-area").slideUp();
    });

    $("input[name=theSecret]").keyup(function(e) {
	if (e.keyCode == 13) {
	    renew_map();
	    $("#secret-change-area").slideUp();
	}
    });

    $("#change-secret").click(function() {
	$("#secret-change-area").slideToggle();
    });

    $("#apply-changes").click(renew_map);

    $("#num-points").keyup(function(e) {
	if(e.keyCode == 13) {
	    renew_map();
	}
    });

    $("#toggle-autorefresh").click(toggle_auto_refresh);

    // toggle_auto_refresh();

    renew_map();
});
