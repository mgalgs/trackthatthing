var insert_test_point = function() {
    var data = {
	secret: $('input[name=secret]').val(),
	lat_step: 0.0001,
	lon_step: 0.0001,
	acc_step: 0,
	speed_step: 0
    };
    var data_url = '/new_test_point?' + $.param(data);
    $.getJSON(data_url, function(response, status, request) {
	$.gritter.add({title:response.title, text:response.msg});
    });
};

var inserting_task_running = false;
var inserting_task_id = null;
var inserting_task_interval = 5000; // ms

$(function() {
    $("#new-test-point").click(insert_test_point);

    $("#start-inserting-test-points").live('click', function() {
	if (inserting_task_running) {
	    clearInterval(inserting_task_id);
	    $(this).children('.ui-button-text').html('Insert More');
	} else {
	    inserting_task_running = true;
	    inserting_task_id = setInterval(insert_test_point, inserting_task_interval)
	    $(this).children('.ui-button-text').html('Stop Inserting');
	}
    });
});
