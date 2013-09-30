var myMakeButton = function(el) {
    var el = this;
    var classes = $(el).attr('class').split(' ');
    var picon = $.grep(classes, function(c,ind) {return /^button-primary\-.*$/.test(c) });
    var sicon = $.grep(classes, function(c,ind) {return /^button-secondary\-.*$/.test(c) });
    var params = {icons: {}};
    if (picon.length > 0) {
	params['icons']['primary'] = 'ui-icon-' + picon[0].replace('button-primary-','');
    }
    if (sicon.length > 0) {
	params['icons']['secondary'] = 'ui-icon-' + sicon[0].replace('button-secondary-','');
    }
    $(this).button(params);
};

$(function() {
   $('button, .button, .disabled-button').each(myMakeButton);
    $('.disabled-button').button('disable');
});
