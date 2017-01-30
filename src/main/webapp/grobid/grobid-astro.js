/**
 *  Javascript functions for the front end.
 *
 *  Author: Patrice Lopez
 */

var grobid = (function ($) {

    // for components view
    var entities = null;

    function defineBaseURL(ext) {
        var baseUrl = null;
        if ($(location).attr('href').indexOf("index.html") != -1)
            baseUrl = $(location).attr('href').replace("index.html", ext);
        else
            baseUrl = $(location).attr('href') + ext;
        return baseUrl;
    }

    function setBaseUrl(ext) {
        var baseUrl = defineBaseURL(ext);
        $('#gbdForm').attr('action', baseUrl);
    }

    $(document).ready(function () {

        $("#subTitle").html("About");
        $("#divAbout").show();
        $("#divRestI").hide();
        $("#divDoc").hide();
        $('#consolidateBlock').show();

        createInputTextArea('text');
        setBaseUrl('processAstroText');
        $('#example0').bind('click', function (event) {
            event.preventDefault();
            $('#inputTextArea').val(examples[0]);
        });
        setBaseUrl('processAstroText');
        $('#example1').bind('click', function (event) {
            event.preventDefault();
            $('#inputTextArea').val(examples[1]);
        });
        $('#example2').bind('click', function (event) {
            event.preventDefault();
            $('#inputTextArea').val(examples[2]);
        });
        $('#example3').bind('click', function (event) {
            event.preventDefault();
            $('#inputTextArea').val(examples[3]);
        });
        $("#selectedService").val('processAstroText');

        $('#selectedService').change(function () {
            processChange();
            return true;
        });

        $('#submitRequest').bind('click', submitQuery);

        $("#about").click(function () {
            $("#about").attr('class', 'section-active');
            $("#rest").attr('class', 'section-not-active');
            $("#doc").attr('class', 'section-not-active');
            $("#demo").attr('class', 'section-not-active');

            $("#subTitle").html("About");
            $("#subTitle").show();

            $("#divAbout").show();
            $("#divRestI").hide();
            $("#divDoc").hide();
            $("#divDemo").hide();
            return false;
        });
        $("#rest").click(function () {
            $("#rest").attr('class', 'section-active');
            $("#doc").attr('class', 'section-not-active');
            $("#about").attr('class', 'section-not-active');
            $("#demo").attr('class', 'section-not-active');

            $("#subTitle").hide();
            //$("#subTitle").show();
            processChange();

            $("#divRestI").show();
            $("#divAbout").hide();
            $("#divDoc").hide();
            $("#divDemo").hide();
            return false;
        });
        $("#doc").click(function () {
            $("#doc").attr('class', 'section-active');
            $("#rest").attr('class', 'section-not-active');
            $("#about").attr('class', 'section-not-active');
            $("#demo").attr('class', 'section-not-active');

            $("#subTitle").html("Doc");
            $("#subTitle").show();

            $("#divDoc").show();
            $("#divAbout").hide();
            $("#divRestI").hide();
            $("#divDemo").hide();
            return false;
        });
        $("#demo").click(function () {
            $("#demo").attr('class', 'section-active');
            $("#rest").attr('class', 'section-not-active');
            $("#about").attr('class', 'section-not-active');
            $("#doc").attr('class', 'section-not-active');

            $("#subTitle").html("Demo");
            $("#subTitle").show();

            $("#divDemo").show();
            $("#divDoc").hide();
            $("#divAbout").hide();
            $("#divRestI").hide();
            return false;
        });
    });

    function ShowRequest(formData, jqForm, options) {
        var queryString = $.param(formData);
        $('#requestResult').html('<font color="grey">Requesting server...</font>');
        return true;
    }

    function AjaxError(jqXHR, textStatus, errorThrown) {
        $('#requestResult').html("<font color='red'>Error encountered while requesting the server.<br/>" + jqXHR.responseText + "</font>");
        responseJson = null;
    }

    function htmll(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function submitQuery() {
        var urlLocal = $('#gbdForm').attr('action');
        {
            $.ajax({
                type: 'GET',
                url: urlLocal,
                data: {text: $('#inputTextArea').val()},
                success: SubmitSuccesful,
                error: AjaxError,
                contentType: false
                //dataType: "text"
            });
        }

        $('#requestResult').html('<font color="grey">Requesting server...</font>');
    }

    function SubmitSuccesful(responseText, statusText) {
        var selected = $('#selectedService option:selected').attr('value');

        if (selected == 'processAstroText') {
            SubmitSuccesfulText(responseText, statusText);
        }
        else if (selected == 'processAstroTEI') {
            //SubmitSuccesfulXML(responseText, statusText);          
        }
        else if (selected == 'processAstroPDF') {
            //SubmitSuccesfulPDF(responseText, statusText);          
        }
        else if (selected == 'annotateAstroPDF') {
            //SubmitSuccesfulPDF(responseText, statusText);          
        }

    }

    function SubmitSuccesfulText(responseText, statusText) {
        responseJson = responseText;
        if ((responseJson == null) || (responseJson.length == 0)) {
            $('#requestResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
            return;
        }

        responseJson = jQuery.parseJSON(responseJson);

        var display = '<div class=\"note-tabs\"> \
            <ul id=\"resultTab\" class=\"nav nav-tabs\"> \
                <li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li> \
                <li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
            </ul> \
            <div class="tab-content"> \
            <div class="tab-pane active" id="navbar-fixed-annotation">\n';

        display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';

        var string = $('#inputTextArea').val();
        var newString = "";
        var lastMaxIndex = string.length;

        display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">';
        //var string = responseJson.text;

        display += '<tr style="background-color:#FFF;">';
        entities = responseJson.entities;      
        if (entities) {
            var pos = 0; // current position in the text

            for (var currentEntityIndex = 0; currentEntityIndex < entities.length; currentEntityIndex++) {
                var entity = entities[currentEntityIndex];
                var entityType = entity.type;
                var entityRawForm = entity.rawForm;
                var start = parseInt(entity.offsetStart, 10);
                var end = parseInt(entity.offsetEnd, 10);
    
                if (start < pos) {
                    // we have a problem in the initial sort of the quantities
                    // the server response is not compatible with the present client 
                    console.log("Sorting of entities as present in the server's response not valid for this client.");
                    // note: this should never happen?
                }
                else {
                    newString += string.substring(pos, start)
                        + ' <span id="annot-' + currentEntityIndex + '" rel="popover" data-color="' + entityType + '">'
                        + '<span class="label ' + entityType + '" style="cursor:hand;cursor:pointer;" >'
                        + string.substring(start, end) + '</span></span>';
                    pos = end;
                }
            }
            newString += string.substring(pos, string.length);
        }

        newString = "<p>" + newString.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
        //string = string.replace("<p></p>", "");

        display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>' + newString + '</p></td>';
        display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';

        display += '</tr>';


        display += '</table>\n';


        display += '</pre>\n';


        display += '</div> \
                    <div class="tab-pane " id="navbar-fixed-json">\n';


        display += "<pre class='prettyprint' id='jsonCode'>";

        display += "<pre class='prettyprint lang-json' id='xmlCode'>";
        var testStr = vkbeautify.json(responseText);

        display += htmll(testStr);

        display += "</pre>";
        display += '</div></div></div>';

        $('#requestResult').html(display);
        window.prettyPrint && prettyPrint();

        

        if (entities) {
            for (var entityIndex = 0; entityIndex < entities.length; entityIndex++) {
                $('#annot-' + entityIndex).bind('hover', viewEntity);
                $('#annot-' + entityIndex).bind('click', viewEntity);
            }
        }

        $('#detailed_annot-0').hide();

        $('#requestResult').show();
    }

    function viewEntity() {
        var localID = $(this).attr('id');
        if (entities == null) {
            return;
        }

        var ind = localID.indexOf('-');
        var localEntityNumber = parseInt(localID.substring(ind+1,localID.length));
        if (localEntityNumber < entities.length) {

            var string = toHtml(entities[localEntityNumber]);

            $('#detailed_annot-0').html(string);
            $('#detailed_annot-0').show();
        }
    }

    function toHtml(entity) {
        var string = "";
        var first = true;
        
        var type = entity.type;

        var colorLabel = null;
        if (type) {
            colorLabel = type;
        } else {
            colorLabel = entity.rawName;
        }

        var rawForm = entity.rawForm;

            string += "<div class='info-sense-box " + colorLabel + "'><h2 style='color:#FFF;padding-left:10px;font-size:16;'>ASTRONOMICAL " + type;
            string += "</h2>";

        string += "<div class='container-fluid' style='background-color:#FFF;color:#70695C;border:padding:5px;margin-top:5px;'>" +
            "<table style='width:100%;display:inline-table;'><tr style='display:inline-table;'><td>";

        if (type) {
            string += "<p>quantity type: <b>" + type + "</b></p>";
        }

        if (rawForm) {
            string += "<p>raw form: <b>" + rawForm + "</b></p>";
        }

        string += "</td></tr>";
        string += "</table></div>";
        string += "</div>";

        return string;
    }

    function processChange() {
        var selected = $('#selectedService option:selected').attr('value');

        if (selected == 'processAstroText') {
            createInputTextArea();
            //$('#consolidateBlock').show();
            setBaseUrl('processAstroText');
        }
        else if (selected == 'processAstroTEI') {
            createInputFile(selected)
            //$('#consolidateBlock').show();
            setBaseUrl('processAstroTEI');
        }
        else if (selected == 'processAstroPDF') {
            createInputFile(selected);
            //$('#consolidateBlock').hide();
            setBaseUrl('processAstroPDF');
        }
        else if (selected == 'annotateAstroPDF') {
            createInputFile(selected);
            //$('#consolidateBlock').hide();
            setBaseUrl('annotateAstroPDF');
        }
    }

    function createInputFile(selected) {
        //$('#label').html('&nbsp;'); 
        $('#textInputDiv').hide();
        //$('#fileInputDiv').fileupload({uploadtype:'file'});
        //$('#fileInputDiv').fileupload('reset');
        $('#fileInputDiv').show();

        $('#gbdForm').attr('enctype', 'multipart/form-data');
        $('#gbdForm').attr('method', 'post');
    }

    function createInputTextArea() {
        //$('#label').html('&nbsp;'); 
        $('#fileInputDiv').hide();
        //$('#input').remove();

        //$('#field').html('<table><tr><td><textarea class="span7" rows="5" id="input" name="'+nameInput+'" /></td>'+
        //"<td><span style='padding-left:20px;'>&nbsp;</span></td></tr></table>");
        $('#textInputDiv').show();

        //$('#gbdForm').attr('enctype', '');
        //$('#gbdForm').attr('method', 'post');
    }

    var examples = ["We detect GRB 020819B at 4σ at 3 GHz, at about 11 yr after the burst. We argue that a good fraction of this emission, if not all, is due to afterglow emission, thus adding GRB 020819B to the group of GRBs with very long-lasting detected radio afterglows, where GRB 030329 is the most prominent example (van der Horst et al. 2008). In a similar case, GRB 980425, with a radio-bright knot at the GRB position, an afterglow interpretation has been excluded (Michałowski et al. 2014).",
        ".",
        ".",
        "."]


})(jQuery);



