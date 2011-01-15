var get_number_of_points = function() {
    return $("#num-points").val();
};

var get_points = function() {
    var secret = $('input[name=theSecret]').val();
    if (secret == '' || secret == $('input[name=theSecret]').attr('title')) {
	secret = $('input[name=secret]').val();
	if (secret == $('input[name=secret]').attr('title')) {
	    $("#secret-change-area").slideDown();
	    return;		// they haven't entered a secred yet...
	}
    }

    $("#current-map-for").html(secret);
    var data_url = '/get?secret='+secret+'&n='+get_number_of_points();

    $.getJSON(data_url, function(obj, retval, xmlhttprequest) {
	var the_coords = [];
	for (var i=0; i<obj.data.locations.length; i++) {
	    the_coords.push(
		new google.maps.LatLng(obj.data.locations[i].latitude,
				       obj.data.locations[i].longitude)
	    );
	}

	var the_path = new google.maps.Polyline({
	    path: the_coords,
	    strokeColor: "#0011AA",
	    strokeOpacity: 0.7,
	    strokeWeight: 5
	});

	the_path.setMap(the_map);
    });
};

var the_map = null;

$(function() {
    the_map = new google.maps.Map($("#map-canvas")[0], {
	zoom: 17,
	center: new google.maps.LatLng(33.989135,-117.339123),
	mapTypeId: google.maps.MapTypeId.HYBRID
    });

    $("#change-secret-go").button().click(function() {
	get_points();
	$("#secret-change-area").slideUp();
    });

    $("#change-secret").click(function() {
	$("#secret-change-area").slideToggle();
    });

    get_points();
});
