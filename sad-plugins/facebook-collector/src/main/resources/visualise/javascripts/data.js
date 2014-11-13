var BASE_URL = "/" + window.location.href.split('/')[3];
$(document).ready(function() {

    var jobid = gup('jobid');
    var num_results = gup('num_results');

    if (num_results.length < 1) {
        num_results = -1;
    } else {
        if (!isNumber(num_results)) {
            num_results = -1;
        }
    }

    $("#data_container").html('<p>Fetching data for job [' + jobid + '], please wait...</p>');

    $.ajax({
        url: BASE_URL + '/jobs/' + jobid + '/data',
        dataType: 'json',
        type: 'GET',
        contentType: "application/json; charset=utf-8",
        error: function() {
            $("#data_container").html('<p>ERROR fetching data for job [' + jobid + ']</p>');
        },
        success: function(data) {
            console.log(data);
            $("#data_container").empty();

            var facebook_posts_raw_collector = [];
            var counter_search = 0;
            $.each(data.response.series, function(key, value) {
                var dataType = value.type;
                if (dataType == "facebook-posts-raw" || dataType == "facebook-posts-topics-tagged") {
                    facebook_posts_raw_collector[counter_search] = value.jsonData;
                    counter_search++;
                }
            });

            var root_container_row, content_container, raw_data_container, json_container, counter = 1,
                    child_container_row;

            if (facebook_posts_raw_collector.length > 0) {
                $("#data_container").append('<h4>Facebook-posts-raw</h4>');
                root_container_row = $('<div class="row"></div>').appendTo("#data_container");
                content_container = $('<div class="six columns"></div>').appendTo(root_container_row);
                if (num_results == -1) {
                    content_container.append('<h5>All entries (' + facebook_posts_raw_collector.length + '):</h5>');
                } else {
                    content_container.append('<h5>First ' + num_results + ' entries, ' + facebook_posts_raw_collector.length + ' total:</h5>');
                }
                counter = 1;
                for (i in facebook_posts_raw_collector) {
                    var facebook_post = facebook_posts_raw_collector[i];
                    child_container_row = $('<div class="row only_bottom_padding"></div>');

                    var userPicContainer = $('<div class="two columns"></div>');
                    var contentContainer = $('<div class="ten columns only_bottom_padding"></div>');

                    userPicContainer.append('<img src="http://graph.facebook.com/' + facebook_post.from.id + '/picture"/>');

                    contentContainer.append('<p class="no_padding"><strong>' + facebook_post.from.name + '</strong> <a href="http://facebook.com/' + facebook_post.id + '" target="_blank">link to post</a></p>');
                    if (typeof (facebook_post.name) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Name: ' + facebook_post.name + '</p>');
                    }
                    if (typeof (facebook_post.message) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Message: ' + facebook_post.message + '</p>');
                    }
                    if (typeof (facebook_post.story) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Story: ' + facebook_post.story + '</p>');
                    }
                    if (typeof (facebook_post.created_time) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Created: ' + isoStringToDate(facebook_post.created_time) + '</p>');
                    }
                    contentContainer.append('<p class="no_padding">Comments: ' +
                            (typeof (facebook_post.comments) != "undefined" ? facebook_post.comments.count : "N/A") +
                            ', likes: ' +
                            (typeof (facebook_post.likes) != "undefined" ? facebook_post.likes.count : "N/A") +
                            '</p>');
                    if (typeof (facebook_post.picture) != "undefined") {
                        contentContainer.append('<p class="no_padding">Shared media:</p>');
                        contentContainer.append('<p class="no_padding"><img src="' + facebook_post.picture.replace("_s.", "_n.") + '"/></p>');
                    }

                    child_container_row.append(userPicContainer);
                    child_container_row.append(contentContainer);
                    content_container.append(child_container_row);
                    counter++;
                    if (num_results > 0) {
                        if (counter > num_results) {
                            break;
                        }
                    }
                }

                raw_data_container = $('<div class="six columns"></div>').appendTo(root_container_row);
                raw_data_container.append('<h5>Raw data of the first entry:</h5>');
                json_container = $('<pre class="resultsContainer"></pre>').appendTo(raw_data_container);
                json_container.html(syntaxHighlight(facebook_posts_raw_collector[0]));
            }
        }
    });

});

function isoStringToDate(s) {
    var b = s.split(/[-t:+]/ig);
    return new Date(Date.UTC(b[0], --b[1], b[2], b[3], b[4], b[5]));
}

function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

function gup(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    if (results == null)
        return "";
    else
        return results[1];
}

function syntaxHighlight(json) {
    if (typeof json != 'string') {
        json = JSON.stringify(json, undefined, 2);
    }
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function(match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}


(function($) {
    $(function() {
        $(document).foundationMediaQueryViewer();

        $(document).foundationAlerts();
        $(document).foundationAccordion();
        $(document).tooltips();
        $('input, textarea').placeholder();

        $(document).foundationButtons();

        $(document).foundationNavigation();

    });

})(jQuery);
