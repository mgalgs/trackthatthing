var get_number_of_points = function() {
    return $("#num-points").val();
};

var get_oldness = function() {
    var val = $("#oldness").val();
    return val !== '' ? val-0 : -1;
};

var get_secret = function() {
    // first try for the one on the page:
    var secret = $('input[name=theSecret]').val();
    if (secret == '' || secret == $('input[name=theSecret]').attr('title')) {
        // try for the hidden input:
        secret = $('input[name=secret]').val();
        if (secret == $('input[name=secret]').attr('title')) {
            $("#secret-change-area").slideDown();
            return null;        // they haven't entered a secred yet...
        }
    }
    return secret;
};

var renew_map = function() {
    var secret = get_secret();
    if (secret === null) {
        return;
    }

    if ( $("#get-started-hint").is(':visible')) {
        $("#get-started-hint").fadeOut();
    }
    if ( $("#maps-n-things").css('visibility') != 'visible' ) {
        $("#maps-n-things").css({visibility:'visible'});
    }
    if ( !$("#live-map-for-container").is(':visible') ) {
        $("#live-map-for-container").fadeIn();
    }

    $("#map-permalink").attr('href', '/live?secret='+secret.replace(/ /g, ''));
    $("#broken-map").attr('href', '/live?secret='+secret.replace(/ /g, ''));

    clear_map();


    // update the spot where the secret is displayed:
    $("#current-map-for").html(secret);

    // get the data and update the map
    var data_url = 'https://trackthatthing.com/get?secret='+secret+'&n='+get_number_of_points()+'&oldness='+get_oldness();
    $.getJSON(data_url, function(obj, retval, xmlhttprequest) {
        if (obj.success) {
            draw_new_points(obj.data.locations);
            recenter_map();
        } else {
            $.gritter.add({title:'Error', text:obj.msg});
        }
    });
};


var attach_info_window = function(pp, data, ind) {
    // the infowindow:
    var id = "info-window-holder-thing-"+ind;
    var target = $('#'+id);
    if (target.length < 1) {
        $('#hide-me').append('<div id="'+id+'"></div>');
        target = $('#'+id);
    }
    target.html( $("#info-window-template").tmpl(data[ind])[0] );
    var iw = new google.maps.InfoWindow({
        // content: 'stufffs'
        content: target[0]
    });
    the_info_windows.push(iw);

    // add listener to show InfoWindow when they click a marker:
    google.maps.event.addListener(pp, 'click', function(e) {
        close_info_windows();
        iw.open(the_map, pp);
    });
};

var close_info_windows = function() {
    for (var i in the_info_windows) {
        the_info_windows[i].close();
    }
};

var draw_new_points = function(data) {
    if (data.length == 0) {
        $("#how-many-points")
            .html(current_overlays.length > 0 ? current_overlays.length-1 : 0);
        return;
    }
    var the_coords = [];
    var the_markers = [];
    var new_data_points = [];
    $.gritter.add({title:'New Data', text:'We just got '+data.length+' new data point'+(data.length==1?'.':'s.')});
    for (i in data) {
        var ll = new google.maps.LatLng(data[i].latitude,
                                        data[i].longitude);
        // add the circle accuracy indicators:
        var mrkr_acc = new google.maps.Circle({
            center: ll,
            clickable: false,
            radius: parseInt(data[i]['accuracy']),
            fillColor: "#DD3333",
            fillOpacity: 0.18,
            strokeOpacity: 0,
            zIndex: 7,
            map: the_map
        });
        // the pushpin:
        var pushpin = new google.maps.Marker({
            icon: 'https://chart.googleapis.com/chart?chst=d_map_pin_icon_withshadow&chld=glyphish_gear|ADDE63',
            map: the_map,
            position: ll,
            title: data[i]['date']+' -> '+i
        });

        attach_info_window(pushpin, data, i);

        the_coords.push(ll);
        new_data_points.push({'raw':data[i],'ll':ll});
        the_markers.push(mrkr_acc);
        the_pushpins.push(pushpin);
    } // eo for i in locations
    new_data_points.reverse();
    data_points = data_points.concat(new_data_points);

    var should_draw_line = true;

    // just in case we only added one point:
    if (new_data_points.length == 1) {
        if (data_points.length > 1) {
            the_coords = [
                data_points[data_points.length-2]['ll'],
                data_points[data_points.length-1]['ll']
            ];
        } else {
            // we only have one point total and it's the new point we
            // got. don't draw a line.
            should_draw_line = false;
        }
    }

    if (should_draw_line) {
        // Draw the line:
        var the_path = new google.maps.Polyline({
            path: the_coords,
            strokeColor: "#0011AA",
            strokeOpacity: 0.7,
            strokeWeight: 5,
            zIndex: 5
        });

        current_overlays.push(the_path);
        the_path.setMap(the_map);
        drew_a_line = true;
    }


    current_overlays = current_overlays.concat(the_markers);

    $("#how-many-points")
        .html(current_overlays.length - (drew_a_line ? 1 : 0));
};


var add_new_points = function() {
    var data = {
        secret:get_secret(),
        n:get_number_of_points(),
        oldness:get_oldness()
    };
    if (data_points.length > 0) {
        data['last'] = data_points[data_points.length-1]['raw'].id;
    }
    var data_url = 'https://trackthatthing.com/get?' + $.param(data);
    $.getJSON(data_url, function(obj, retval, xmlhttprequest) {
        if (!obj.success) {
            $.gritter.add({title:'Error', text:obj.msg});
            return;
        }
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
        current_overlays[i] = null;
    }
    for (i in the_pushpins) {
        the_pushpins[i].setMap(null);
        the_pushpins[i] = null;
    }
    current_overlays = [];
    data_points = [];
    the_pushpins = [];
};

var disable_auto_refresh = function () {
    var btn = $("#toggle-autorefresh");
    refreshing_task_is_running = false;
    clearInterval(refreshing_task_id);
    btn.children('.ui-button-text').html('Enable Auto-update');
    $.gritter.add({title:'Notice', text:'Auto-update disabled.'})
};

var enable_auto_refresh = function () {
    var btn = $("#toggle-autorefresh");
    refreshing_task_is_running = true;
    refreshing_task_id = setInterval(add_new_points, refreshing_task_period);
    btn.children('.ui-button-text').html('Disable Auto-update');
    $.gritter.add({title:'Notice', text:'Auto-update enabled.'})
};

var toggle_auto_refresh = function() {
    if (refreshing_task_is_running) {
        disable_auto_refresh();
    } else {
        enable_auto_refresh();
    }
};

var start_inactivity_timer = function() {
    window.setTimeout(function() {
        if (refreshing_task_is_running) {
            disable_auto_refresh();
            $("#inactive-dialog").dialog({
                modal: true,
                buttons: [{
                    text: "Ok",
                    click: function() {
                        $(this).dialog("close");
                        // re-enable autoupdate
                        enable_auto_refresh();
                        start_inactivity_timer();
                    }
                }]
            });
        }
    }, INACTIVE_TIMEOUT * 60 * 1000);
};


// data_points variable: all the data. will maintain order with oldest
// item in slot 0, newest at the back
var data_points = [];
var current_overlays = [];
var the_info_windows = [];
var the_pushpins = [];
var the_map = null;
var drew_a_line = false;

var refreshing_task_is_running = false;
var refreshing_task_id = null;
var refreshing_task_period = 7500; // ms
var inactive_timer_id = -1;
var INACTIVE_TIMEOUT = 30; // minutes


// main function:
$(function() {
    the_map = new google.maps.Map($("#map-canvas")[0], {
        zoom: 17,
        center: new google.maps.LatLng(33.989135,-117.339123),
        mapTypeId: google.maps.MapTypeId.HYBRID
    });


    // we use a custom event ('ttt-secret-changed') on the "body"
    // element to trigger when the secret gets changed.

    $("#change-secret-go").button().click(function() {
        $("body").trigger('ttt-secret-changed');
        $("#secret-change-area").slideUp();
    });

    $("input[name=theSecret]").keyup(function(e) {
        if (e.keyCode == 13) {
            $("body").trigger('ttt-secret-changed');
            $("#secret-change-area").slideUp();
        }
    });

    $("body").bind('ttt-secret-changed', renew_map);


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

    google.maps.event.addListener(the_map, 'click', close_info_windows);

    if (null === get_secret()) {
        // show the 'get started' hint:
        var login_url = $("#the-login-link").attr('href');
        var pos = $("#secret-change-area").offset();
        var n = $('<div/>')
            .html('<div><img style="padding-left:20px;" src="/resources/images/arrow.png" /></div><div>enter a secret to track someone else, or <a href="'+login_url+'">login</a>.</div>')
            .attr('id', 'get-started-hint')
            .css({
                position:'absolute',
                'text-align': 'left',
                top:pos.top+150,
                left:pos.left// ,
                // background:'white',
                // padding: '20px',
                // width: '500px',
                // height: '150px'
            })
            .appendTo("body");
        $("body").one('ttt-secret-changed', function() {
            if (!refreshing_task_is_running) {
                toggle_auto_refresh();
            }
        });
    }

    renew_map();

    // set up the inactivity timer:
    start_inactivity_timer();

    enable_auto_refresh();
});
