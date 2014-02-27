/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
$(document).ready(function() {

    var isisVeilTimeoutId;
    
    isisShowVeil = function() {
        if(isisVeilTimeoutId) {
            clearTimeout(isisVeilTimeoutId);
            isisVeilTimeoutId = null;
        }
        $("#veil").stop().show();
    }
    isisFadeInVeil = function(attributes, jqxhr, settings) {
        // use timeouts because JQuery's delay(...) cannot be stopped. 
        var activeEl = attributes.currentTarget.activeElement;
        isisVeilTimeoutId = setTimeout(function() {
            $("#veil").fadeIn(750);
        }, 250);
        
    }
    isisHideVeil = function(attributes, jqXHR, settings) {
        if(isisVeilTimeoutId) {
            clearTimeout(isisVeilTimeoutId);
            isisVeilTimeoutId = null;
        }
        $("#veil").stop().hide();
    }
    
    isisOpenInNewTab = function(url){
    	var win=window.open(url, '_blank'); 
    	if(win) { win.focus(); }
	}

    /* for modal dialogs */
    Wicket.Event.subscribe(
            '/ajax/call/beforeSend', function(attributes, jqXHR, settings) {
                isisFadeInVeil(attributes, jqXHR, settings);
            });
    Wicket.Event.subscribe(
            '/ajax/call/complete', function(attributes, jqXHR, settings) {
                isisHideVeil(attributes, jqXHR, settings);
            });
    

    
    /* only seem to work in non-modal situation */
    $('.buttons .okButton:not(.noVeil)').click(isisShowVeil);
    $('.buttons .ok:not(.noVeil)').click(isisShowVeil);
    $('.cssSubMenuItemsPanel .cssSubMenuItem a:not(.noVeil)').click(isisShowVeil);

    $('div.collectionContentsAsAjaxTablePanel > table.contents > tbody > tr.reloaded-after-concurrency-exception') 
        .livequery(function(){
            x=$(this);
            $(this).animate({ "backgroundColor": "#FFF" }, 1000, "linear", function() {
                $(x).css('background-color','').removeClass("reloaded-after-concurrency-exception");
            }); 
        }); 
    
    
    
//    $('a#zclip-copy').zclip({
//        path:'/wicket/wicket/resource/org.apache.isis.viewer.wicket.ui.pages.PageAbstract/ZeroClipboard.swf',
//        copy:$('a#zclip-source').attr("href")
//    });
//
//    $('a#zclip-copy').zclip('show');

    
});

/**
 * enables 'maxlength' to work as an attribute on 'textarea'
 * 
 * as per: see http://stackoverflow.com/questions/4459610/set-maxlength-in-html-textarea
 */
$(function() {  
    $("textarea[maxlength]").bind('input propertychange', function() {  
        var maxLength = $(this).attr('maxlength');  
        if ($(this).val().length > maxLength) {  
            $(this).val($(this).val().substring(0, maxLength));  
        }  
    })  
});
