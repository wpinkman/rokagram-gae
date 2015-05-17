$( document ).ready(function() {
	
	$("#placeholder").css('height', $('#placeholder').width())
    
	$('.busy-indicator').hide();
	
    $('.user-search-form').submit(function(){
    	var mbody = $(this).closest('.modal-body');
    	mbody.find('#results').empty()
    	mbody.find('.busy-indicator').show();
    	url = '/love?' + $(this).serialize()
    	mbody.find('#results').load(url, function() {
    		mbody.find('.busy-indicator').hide();
    		
    	    $('.user-choose').click(function(e) {
    	    	username = $(this).closest('.userthumb').data('username')
    	    	myObj = $.deparam.querystring();
    	    	myObj[$('#toorfrom').val()] = username
    	    	location = '/love?' + $.param(myObj);
    	    	
    	    })
    	});
    	return false;
    });
    
    $('.modal-opener').click(function(){
    	
    	var searchModal = $('#search-user-modal')
    	searchModal.find('#toorfrom').val($(this).data('toorfrom'));
    	
    	searchModal.modal();
    	
    	return false;
    });
    
    $('#swap').click(function(){
    	oldObj = $.deparam.querystring();
    	
    	newObj = {}
    	
    	if (oldObj.from) {
    		newObj['to'] = oldObj.from
    	}
    	if (oldObj.to) {
    		newObj['from'] = oldObj.to
    	}
    	location = '/love?' + $.param(newObj);
    	
    	return false;
    });
    
    
});