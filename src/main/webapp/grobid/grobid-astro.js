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
        //$('#consolidateBlock').show();

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
    });

    function ShowRequest(formData, jqForm, options) {
        var queryString = $.param(formData);
        $('#infoResult').html('<font color="grey">Requesting server...</font>');
        return true;
    }

    function AjaxError(jqXHR, textStatus, errorThrown) {
        $('#infoResult').html("<font color='red'>Error encountered while requesting the server.<br/>" + jqXHR.responseText + "</font>");
        responseJson = null;
    }

    function AjaxError2(message) {
        if (!message)
            message = "";
        message += " - The PDF document cannot be annotated. Please check the server logs.";
        $('#infoResult').html("<font color='red'>Error encountered while requesting the server.<br/>"+message+"</font>");
        responseJson = null;
        return true;
    }

    function htmll(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function submitQuery() {
        $('#infoResult').html('<font color="grey">Requesting server...</font>');
        $('#requestResult').html('');

        var selected = $('#selectedService option:selected').attr('value');
        if (selected == 'processAstroText') {
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
        }
        else if (selected == 'annotateAstroPDF') {
            // we will have JSON annotations to be layered on the PDF

            // request for the annotation information
            var form = document.getElementById('gbdForm');
            var formData = new FormData(form);
            var xhr = new XMLHttpRequest();
            var url = $('#gbdForm').attr('action');
            xhr.responseType = 'json'; 
            xhr.open('POST', url, true);
            //ShowRequest2();

            var nbPages = -1;

            // display the local PDF
            if ((document.getElementById("input").files[0].type == 'application/pdf') ||
                (document.getElementById("input").files[0].name.endsWith(".pdf")) ||
                (document.getElementById("input").files[0].name.endsWith(".PDF")))
                var reader = new FileReader();
            reader.onloadend = function () {
                // to avoid cross origin issue
                //PDFJS.disableWorker = true;
                var pdfAsArray = new Uint8Array(reader.result);
                // Use PDFJS to render a pdfDocument from pdf array
                PDFJS.getDocument(pdfAsArray).then(function (pdf) {
                    // Get div#container and cache it for later use
                    var container = document.getElementById("requestResult");
                    // enable hyperlinks within PDF files.
                    //var pdfLinkService = new PDFJS.PDFLinkService();
                    //pdfLinkService.setDocument(pdf, null);

                    //$('#requestResult').html('');
                    nbPages = pdf.numPages;

                    // Loop from 1 to total_number_of_pages in PDF document
                    for (var i = 1; i <= nbPages; i++) {

                        // Get desired page
                        pdf.getPage(i).then(function (page) {

                            var div0 = document.createElement("div");
                            div0.setAttribute("style", "text-align: center; margin-top: 1cm;");
                            var pageInfo = document.createElement("p");
                            var t = document.createTextNode("page " + (page.pageIndex + 1) + "/" + (nbPages));
                            pageInfo.appendChild(t);
                            div0.appendChild(pageInfo);
                            container.appendChild(div0);

                            var scale = 1.5;
                            var viewport = page.getViewport(scale);
                            var div = document.createElement("div");

                            // Set id attribute with page-#{pdf_page_number} format
                            div.setAttribute("id", "page-" + (page.pageIndex + 1));

                            // This will keep positions of child elements as per our needs, and add a light border
                            div.setAttribute("style", "position: relative; border-style: solid; border-width: 1px; border-color: gray;");

                            // Append div within div#container
                            container.appendChild(div);

                            // Create a new Canvas element
                            var canvas = document.createElement("canvas");

                            // Append Canvas within div#page-#{pdf_page_number}
                            div.appendChild(canvas);

                            var context = canvas.getContext('2d');
                            canvas.height = viewport.height;
                            canvas.width = viewport.width;

                            var renderContext = {
                                canvasContext: context,
                                viewport: viewport
                            };

                            // Render PDF page
                            page.render(renderContext).then(function () {
                                // Get text-fragments
                                return page.getTextContent();
                            })
                                .then(function (textContent) {
                                    // Create div which will hold text-fragments
                                    var textLayerDiv = document.createElement("div");

                                    // Set it's class to textLayer which have required CSS styles
                                    textLayerDiv.setAttribute("class", "textLayer");

                                    // Append newly created div in `div#page-#{pdf_page_number}`
                                    div.appendChild(textLayerDiv);

                                    // Create new instance of TextLayerBuilder class
                                    var textLayer = new TextLayerBuilder({
                                        textLayerDiv: textLayerDiv,
                                        pageIndex: page.pageIndex,
                                        viewport: viewport
                                    });

                                    // Set text-fragments
                                    textLayer.setTextContent(textContent);

                                    // Render text-fragments
                                    textLayer.render();
                                });
                        });
                    }
                });
            }
            reader.readAsArrayBuffer(document.getElementById("input").files[0]);

            xhr.onreadystatechange = function (e) {
                if (xhr.readyState == 4 && xhr.status == 200) {
                    var response = e.target.response;
                    //var response = JSON.parse(xhr.responseText);
                    //console.log(response);
                    setupAnnotations(response);
                } else if (xhr.status != 200) {
                    AjaxError2("Response " + xhr.status + ": ");
                }
            };
            xhr.send(formData);
        }
    }

    function SubmitSuccesful(responseText, statusText) {
        var selected = $('#selectedService option:selected').attr('value');

        if (selected == 'processAstroText') {
            SubmitSuccesfulText(responseText, statusText);
        } else if (selected == 'annotateAstroPDF') {
            SubmitSuccesfulPDF(responseText, statusText);          
        }

    }

    function SubmitSuccesfulText(responseText, statusText) {
        responseJson = responseText;
        if ((responseJson == null) || (responseJson.length == 0)) {
            $('#infoResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
            return;
        } else {
            $('#infoResult').html('');
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

    function setupAnnotations(response) {
        // we must check/wait that the corresponding PDF page is rendered at this point
        if ((response == null) || (response.length == 0)) {
            $('#infoResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
            return;
        } else {
            $('#infoResult').html('');
        }

        var json = response;
        var pageInfo = json.pages;

        var page_height = 0.0;
        var page_width = 0.0;

        var entities = json.entities;
        if (entities) {
            for(var n in entities) {
                var annotation = entities[n];
                var theId = annotation.rawForm;
                var theUrl = null;
                //var theUrl = annotation.url;
                var pos = annotation.boundingBoxes;
                pos.forEach(function(thePos, m) {
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateEntity(theId, thePos, theUrl, page_height, page_width);
                });
            }
        }
    }

    function annotateEntity(theId, thePos, theUrl, page_height, page_width) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x - 1;
        var y = thePos.y * scale_y - 1 ;
        var width = thePos.w * scale_x + 1;
        var height = thePos.h * scale_y + 1;

        //make clickable the area
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+
            y+"px; left:"+x+"px;";
        element.setAttribute("style", attributes + "border:2px solid; border-color: #800080;");
        element.setAttribute("data-toggle", "popover");
        element.setAttribute("data-placement", "top");
        element.setAttribute("data-content", "content");
        element.setAttribute("data-trigger", "hover");
        $(element).popover({
            content: "<p>Astronomical Object</p><p>" +theId+"<p>",
            html: true,
            container: 'body'
        });
        
        pageDiv.append(element);
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
        /*else if (selected == 'processAstroTEI') {
            createInputFile(selected)
            //$('#consolidateBlock').show();
            setBaseUrl('processAstroTEI');
        }
        else if (selected == 'processAstroPDF') {
            createInputFile(selected);
            //$('#consolidateBlock').hide();
            setBaseUrl('processAstroPDF');
        }*/
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



