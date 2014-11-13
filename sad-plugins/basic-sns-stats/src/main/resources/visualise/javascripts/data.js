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

    // Get plugins query
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

            // Populate data into two arrays depending on type
            var twitter_static_search_raw_collector = [], twitter_basic_stats_collector = [];
            var facebook_basic_stats_collector = [], facebook_posts_raw_collector = [];
            var media_links_with_descriptions_collector = [];
            var counter_twitter_raw = 0, counter_twitter_stats = 0;
            var counter_facebook_raw = 0, counter_facebook_stats = 0, counter_media_links_with_descriptions = 0;
            $.each(data.response.series, function(key, value) {
                var dataType = value.type;
                if (dataType == "twitter-basic-stats") {
                    twitter_basic_stats_collector[counter_twitter_stats] = value.jsonData;
                    counter_twitter_stats++;
                }
                if (dataType == "twitter-static-search-raw") {
                    twitter_static_search_raw_collector[counter_twitter_raw] = value.jsonData;
                    counter_twitter_raw++;
                }
                if (dataType == "facebook-basic-stats") {
                    facebook_basic_stats_collector[counter_facebook_stats] = value.jsonData;
                    counter_facebook_stats++;
                }
                if (dataType == "facebook-posts-raw") {
                    facebook_posts_raw_collector[counter_facebook_raw] = value.jsonData;
                    counter_facebook_raw++;
                }
                if (dataType == "media-links-with-descriptions") {
                    media_links_with_descriptions_collector[counter_media_links_with_descriptions] = value.jsonData;
                    counter_media_links_with_descriptions++;
                }
            });

            var root_container_row, content_container, raw_data_container, json_container, counter = 1,
                    child_container_row, stats_container;

            if (twitter_basic_stats_collector.length > 0) {
                $("#data_container").append('<h4>Data type: twitter-basic-stats</h4>');
                root_container_row = $('<div class="row"></div>').appendTo("#data_container");
                content_container = $('<div class="six columns"></div>').appendTo(root_container_row);
                if (num_results == -1) {
                    content_container.append('<h5>All entries (' + twitter_basic_stats_collector.length + '):</h5>');
                } else {
                    content_container.append('<h5>First ' + num_results + ' entries, ' + twitter_basic_stats_collector.length + ' total:</h5>');
                }
                for (i in twitter_basic_stats_collector) {
                    var value = twitter_basic_stats_collector[i];
                    child_container_row = $('<div class="row"></div>');
                    stats_container = $('<div class="twelve columns"></div>').appendTo(child_container_row);

                    stats_container.append('<p class="no_padding">Unique tweets: <strong>' + value.unique_tweets + '</strong></p>');
                    stats_container.append('<p class="no_padding">Unique users: <strong>' + value.unique_users + '</strong></p>');
                    stats_container.append('<p class="no_padding">Unique languages: <strong>' + value.unique_languages + '</strong></p>');
                    stats_container.append('<p class="no_padding">Unique sources: <strong>' + value.unique_sources + '</strong></p>');

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
                json_container.html(syntaxHighlight(twitter_basic_stats_collector[0]));
                if (facebook_basic_stats_collector.length > 0) {
                    $("#data_container").append('<hr>');
                }
            }

            if (facebook_basic_stats_collector.length > 0) {
                $("#data_container").append('<h4>Data type: facebook-basic-stats</h4>');
                root_container_row = $('<div class="row"></div>').appendTo("#data_container");
                content_container = $('<div class="six columns"></div>').appendTo(root_container_row);
                if (num_results == -1) {
                    content_container.append('<h5>All entries (' + facebook_basic_stats_collector.length + '):</h5>');
                } else {
                    content_container.append('<h5>First ' + num_results + ' entries, ' + facebook_basic_stats_collector.length + ' total:</h5>');
                }
                for (i in facebook_basic_stats_collector) {
                    value = facebook_basic_stats_collector[i];
                    child_container_row = $('<div class="row"></div>');
                    stats_container = $('<div class="twelve columns"></div>').appendTo(child_container_row);

                    stats_container.append('<p class="no_padding">Unique posts: <strong>' + value.unique_posts + '</strong></p>');
                    stats_container.append('<p class="no_padding">Unique users: <strong>' + value.unique_users + '</strong></p>');

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
                json_container.html(syntaxHighlight(facebook_basic_stats_collector[0]));
                if (twitter_static_search_raw_collector.length > 0) {
                    $("#data_container").append('<hr>');
                }
            }

            if (twitter_static_search_raw_collector.length > 0) {
                $("#data_container").append('<h4>Data type: twitter-static-search-raw</h4>');
                root_container_row = $('<div class="row"></div>').appendTo("#data_container");
                content_container = $('<div class="six columns"></div>').appendTo(root_container_row);
                if (num_results == -1) {
                    content_container.append('<h5>All entries (' + twitter_static_search_raw_collector.length + '):</h5>');
                } else {
                    content_container.append('<h5>First ' + num_results + ' entries, ' + twitter_static_search_raw_collector.length + ' total:</h5>');
                }
                counter = 1;
                for (i in twitter_static_search_raw_collector) {
                    var tweet = twitter_static_search_raw_collector[i];
                    child_container_row = $('<div class="row only_bottom_padding"></div>');

                    var userPicContainer = $('<div class="two columns"></div>');
                    var tweetContainer = $('<div class="ten columns only_bottom_padding"></div>');

                    userPicContainer.append('<img src="' + tweet.user.profile_image_url + '"/>');

                    tweetContainer.append('<p class="no_padding"><strong>' + tweet.user.name + '</strong> <a href="http://twitter.com/' + tweet.user.screen_name + '">@' + tweet.user.screen_name + '</p>');
                    tweetContainer.append('<p class="no_padding break_word">' + tweet.text + '</p>');
                    tweetContainer.append('<p class="no_padding">Created: ' + tweet.created_at + '</p>');
                    tweetContainer.append('<p class="no_padding">Source: ' + tweet.source + ', language: ' + tweet.lang + '</p>');
                    tweetContainer.append('<p class="no_padding">Followers ' + tweet.user.followers_count +
                            ', following ' + tweet.user.friends_count +
                            ', listed ' + tweet.user.listed_count +
                            ', favourites ' + tweet.user.favourites_count +
                            '</p>');
                    if (tweet.place != null) {
                        if (tweet.place.full_name != null) {
                            tweetContainer.append('<p class="no_padding">Tweet location: ' + tweet.place.full_name + '</p>');
                        }
                    }
                    if (tweet.user.location.length > 1) {
                        tweetContainer.append('<p class="no_padding">User location: ' + tweet.user.location + '</p>');
                    }
                    if (typeof (tweet.entities.media) != "undefined") {
                        tweetContainer.append('<p class="no_padding">Shared media:</p>');
                        $.each(tweet.entities.media, function(key, media) {
                            if (media.type == "photo") {
                                tweetContainer.append('<p class="no_padding"><img src="' + media.media_url + '"/></p>');
                            }
                        });
                    }
                    if (tweet.buzzScore != null) {
                        tweetContainer.append('<p class="no_padding">Buzz score: ' + tweet.buzzScore + '</p>');
                    }

                    child_container_row.append(userPicContainer);
                    child_container_row.append(tweetContainer);
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
                json_container.html(syntaxHighlight(twitter_static_search_raw_collector[0]));
                if (facebook_posts_raw_collector.length > 0) {
                    $("#data_container").append('<hr>');
                }
            }

            if (facebook_posts_raw_collector.length > 0) {
                $("#data_container").append('<h4>Type: facebook-posts-raw</h4>');
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

                    userPicContainer = $('<div class="two columns"></div>');
                    var contentContainer = $('<div class="ten columns only_bottom_padding"></div>');

                    userPicContainer.append('<img src="http://graph.facebook.com/' + facebook_post.from.id + '/picture"/>');

                    contentContainer.append('<p class="no_padding"><strong>' + facebook_post.from.name + '</strong> <a href="http://facebook.com/' + facebook_post.id + '" target="_blank">link</a></p>');
                    if (typeof (facebook_post.name) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Name: ' + facebook_post.name + '</p>');
                    }
                    if (typeof (facebook_post.type) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Type: ' + facebook_post.type + '</p>');
                    }
                    if (typeof (facebook_post.message) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Message: ' + facebook_post.message + '</p>');
                    }
                    if (typeof (facebook_post.story) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Story: ' + facebook_post.story + '</p>');
                    }
                    if (typeof (facebook_post.created_time) != "undefined") {
                        contentContainer.append('<p class="no_padding break_word">Created: ' + facebook_post.created_time + '</p>');
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
                if (media_links_with_descriptions_collector.length > 0) {
                    $("#data_container").append('<hr>');
                }
            }

            if (media_links_with_descriptions_collector.length > 0) {
                $("#data_container").append('<h4>Type: media-links-with-descriptions</h4>');
                root_container_row = $('<div class="row"></div>').appendTo("#data_container");
                content_container = $('<div class="six columns"></div>').appendTo(root_container_row);
                if (num_results == -1) {
                    content_container.append('<h5>All entries (' + media_links_with_descriptions_collector.length + '):</h5>');
                } else {
                    content_container.append('<h5>First ' + num_results + ' entries, ' + media_links_with_descriptions_collector.length + ' total:</h5>');
                }
                counter = 1;
                for (i in media_links_with_descriptions_collector) {
                    var link_with_text = media_links_with_descriptions_collector[i];

                    child_container_row = $('<div class="row only_bottom_padding"></div>');
                    var linkTextContainer = $('<div class="twelve columns"></div>');

                    linkTextContainer.append('<p class="no_padding break_word">' + link_with_text.text + '</p>');
                    linkTextContainer.append('<p><img src="' + link_with_text.media_url.replace("_s.", "_n.") + '" /></p>');


                    child_container_row.append(linkTextContainer);
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
                json_container.html(syntaxHighlight(media_links_with_descriptions_collector[0]));
            }
        }
    });

});

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
