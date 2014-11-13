var pollingIntervalInSeconds = 5;
var BASE_URL = "/" + window.location.href.split('/')[3];

var sampleSchedule = {
    schedule: {
        times: 1,
        withIntervalInMilliseconds: 20000
    }
};
var DEFAULT_SCHEDULE = JSON.stringify(sampleSchedule, null, 2);
var runPluginQuery = {
    pluginName: 'itinnovation-twitter-hashtag-tracker',
    arguments: {
        keyword: "#BumpMeJanoskiansTix"
    },
    schedule: {
        times: 1,
        withIntervalInMilliseconds: 20000
    }
};
var jobPollingOn = false;
var tracking_job_id = -1;

$(document).ready(function() {

// check if the service is started already.
    $.ajax({
        type: 'GET',
        url: BASE_URL + "/configuration/ifstarted",
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(jqXHR);
            console.log(textStatus);
            console.log(errorThrown);
            $('#configStatus').attr('class', 'alert-color');
            $('#configStatus').text("Service Initialisation Error (" + errorThrown + ")");
        },
        success: function(ifstart_data) {
            console.log(ifstart_data);
            if (ifstart_data === true) {
                // Get plugins query
                $.ajax({
                    url: BASE_URL + '/plugins',
                    type: 'GET',
                    error: function() {
                        $("#pluginsResponseText").text("Error getting plugins");
                    },
                    success: function(data) {
                        console.log(data);
                        var counter = 0;
                        for (pluginName in data.response) {
                            var selected;
                            if (counter === 0) {
                                selected = ' SELECTED';
                                $('#pluginInfo').text(data.response[pluginName].description);
                                updateJsonRequest(data.response[pluginName]);
                            } else {
                                selected = '';
                            }
                            var option = $('<option' + selected + ' id="' + pluginName + '">' + pluginName + '</option>').appendTo($('#pluginsDropdown'));
                            option.data("data", data.response[pluginName]);
                            counter++;
                        }
//            $(document).foundationCustomForms();
                        $("#requestContainer").val(JSON.stringify(runPluginQuery, undefined, 2));
                        $('#pluginsDropdown').change(function() {
                            pluginSelected = $("#pluginsDropdown option:selected");
                            data = pluginSelected.data("data");
                            $('#pluginInfo').text(data.description);
                            updateJsonRequest(data);
                            $("#requestContainer").val(JSON.stringify(runPluginQuery, undefined, 2));
                        });
                    }
                });
                // JOBS
                getJobs();
            } else {
                window.location.replace(BASE_URL + "/index.html");
            }
        }});
    function updateJsonRequest(pluginData) {
        var argumentsAsArray = new Array();
        var inputsAsArray = new Array();
        var outputsAsArray = new Array();
        $.each(pluginData.arguments, function(key, value) {
            var temp = new Object();
            temp[value.name] = value.defaultValue;
            argumentsAsArray[key] = temp;
        });
        $.each(pluginData.inputs, function(key, value) {
            var temp = new Object();
            temp[value.source] = value.defaultValue;
            inputsAsArray[key] = temp;
        });
        $.each(pluginData.outputs.data, function(key, value) {
            var temp = new Object();
            temp.type = value.type;
            outputsAsArray[key] = temp;
        });
        runPluginQuery = {
            pluginName: pluginData.name,
            arguments: argumentsAsArray,
            inputs: inputsAsArray,
            outputs: outputsAsArray,
            schedule: sampleSchedule.schedule
        };
        $("#pluginRunRequestText").html(syntaxHighlight(JSON.stringify(runPluginQuery, undefined, 2)));
    }

    $('#requestContainer').blur(function() {
        runPluginQuery = JSON.parse($(this).val());
        $("#pluginRunRequestText").html(syntaxHighlight(JSON.stringify(runPluginQuery, undefined, 2)));
    });

    $("#resetButton").click(function(e) {
        e.preventDefault();
        $("#reset-sad").foundation('reveal', 'open');
    });

    var resetModal = $('<div id="reset-sad" class="reveal-modal small" data-reveal>').appendTo("#misc");
    resetModal.append("<h2>Hold on!</h2>");
    resetModal.append("<p>This will suspend all running jobs and disconnect from the database and ECC. Click red button to confirm, \"&#215;\" in the top right corner, Esc or outside this dialog to cancel</p>");
    resetModal.append('<a class="close-reveal-modal">&#215;</a>');
    var doResetButton = $('<a class="delete-job-modal button alert">I understand, reset SAD</a>').appendTo(resetModal);
    doResetButton.click(function(e) {
        e.preventDefault();
        $.ajax({
            type: 'GET',
            url: BASE_URL + "/configuration/restart",
            error: function(jqXHR, textStatus, errorThrown) {
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
                $('#configStatus').attr('class', 'alert-color');
                $('#configStatus').text("Failed to restart service (" + errorThrown + ")");
            },
            success: function(response) {
                window.location.replace(BASE_URL + "/index.html");
            }});
    });
    $("#runPluginRequestButton").click(function(e) {
        e.preventDefault();
        console.log(JSON.stringify(runPluginQuery));
        $.ajax({
            url: BASE_URL + '/q/run',
            dataType: 'json',
            type: 'POST',
//            data: JSON.stringify({pluginName: "itinnovation.test", arguments: {snsName: "Twitter", keyword: "football"}}),
            data: JSON.stringify(runPluginQuery),
            contentType: "application/json; charset=utf-8",
            error: function() {
                $("#pluginRunResponseText").text("Error running a plugin");
                jobPollingOn = false;
            },
            success: function(data) {
                console.log(data);
                $(".hiddenResponse").removeClass('hiddenResponse');
                $("#pluginRunResponseText").html(syntaxHighlight(JSON.stringify(data, undefined, 2)));
                jobPollingOn = true;
                tracking_job_id = data.response.ID;
                console.log(tracking_job_id);
                getJobs();
                pollJobStatus();
            }
        });
    });
});
function getJobs() {
    $("#jobs_container").empty();
    var header_topic_container_row = $('<div class="row header"></div>').appendTo("#jobs_container");
    var column_names_container = $('<div class="small-12 columns"></div>');
    var column_names_container_row = $('<div class="row"></div>');
    column_names_container_row.append('<div class="small-7 column"><p>' + 'Job ID &mdash; Plugin name and data/details links' + '</p></div>');
    column_names_container_row.append('<div class="small-4 column"><p>' + 'Timestamps' + '</p></div>');
    column_names_container_row.append('<div class="small-1 column"><p>' + 'Controls' + '</p></div>');
//    column_names_container_row.append('<div class="small-2 column"><p>' + 'Created' + '</p></div>');
//    column_names_container_row.append('<div class="small-2 column"><p>' + 'Last run' + '</p></div>');
    column_names_container.append(column_names_container_row);
    header_topic_container_row.append(column_names_container);
    $.ajax({
        url: BASE_URL + '/jobs?num=10',
//        dataType: 'json',
        type: 'GET',
//        contentType: "application/json; charset=utf-8",
        error: function() {
            $("#pluginRunResponseText").text("Error getting jobs");
            jobPollingOn = false;
        },
        success: function(data) {
//            console.log(data);
//            var printdata = data;
//            console.log(printdata);

            $("#jobsCounter").text(" (" + data.response.num + ")");
            if (data.response.num > 0) {
                var jobs_container_row = $('<div class="row"></div>').appendTo("#jobs_container");
                var jobs_container = $('<div class="small-12 columns" id="jobs_container_div"></div>').appendTo(jobs_container_row);
                var jobsSorted = data.response.list.splice(0).sort(function(a, b) {
                    return Number(b.WhenCreated_in_msec) - Number(a.WhenCreated_in_msec);
                });
                $.each(jobsSorted, function(key, job) {
                    var single_job_row = $('<div class="row" id="' + job.ID + '"></div>');
                    console.log(job);
                    jobs_container.append(buildJobDetailsTableRow(single_job_row, job));
                });
            }
        }
    });
}

function buildJobDetailsTableRow(single_job_row, job) {

    var whenLastRun;
    if (typeof (job.WhenLastrun_as_string) !== "undefined") {
        whenLastRun = job.WhenLastrun_as_string;
    } else {
        whenLastRun = '-';
    }
    single_job_row.append('<div class="large-7 columns"><p>' + job.ID + " &mdash; " + job.PluginName + '<br>' +
            'Status: ' + job.Status + '<br>' +
            'Executions: ' + job.Executions_num + '<br>' +
            'Data entries: ' + job.data_num + '<br>' +
            ' [<a target="_blank" href="' + job.url_data + '">raw data</a>]' +
            ' [<a target="_blank" href="' + job.url_vis + '">visualized data</a>]' +
            ' [<a target="_blank" href="' + job.url_details + '">details</a>]' +
            '</p></div>');
    single_job_row.append('<div class="large-4 columns"><p>Created: ' + job.WhenCreated_as_string + '<br>' +
            'Last run: ' + whenLastRun +
            '</p></div>');
    var controls = $('<div class="large-1 columns"></div>').appendTo(single_job_row);
    var pauseControl = $('<a href="#" class="jobControl">Pause</a>');
    pauseControl.data("url", job.url_pause);
    pauseControl.click(function(e) {
        e.preventDefault();
        $.getJSON($(this).data("url"), function(data) {
            console.log(data);
            getJobs();
        });
    });
    var resumeControl = $('<a href="#" class="jobControl">Resume</a>');
    resumeControl.data("url", job.url_resume);
    resumeControl.click(function(e) {
        e.preventDefault();
        $.getJSON($(this).data("url"), function(data) {
            console.log(data);
            getJobs();
        });
    });
    var cancelControl = $('<a href="#">Cancel</a>');
    cancelControl.data("url", job.url_cancel);
    cancelControl.click(function(e) {
        e.preventDefault();
        $.getJSON($(this).data("url"), function(data) {
            console.log(data);
            jobPollingOn = false;
            getJobs();
        });
    });
    var deleteModal = $('<div id="delete-job-confirmation-' + job.ID + '" class="reveal-modal small" data-reveal>').appendTo(controls);
    deleteModal.append("<h2>Hold on!</h2>");
    deleteModal.append("<p>This will delete job [" + job.ID + "] and ALL its data. Click red button to confirm, \"&#215;\" in the top right corner, Esc or outside this dialog to cancel</p>");
    deleteModal.append('<a class="close-reveal-modal">&#215;</a>');
    var doDeleteButton = $('<a class="delete-job-modal button alert">I understand, delete all data</a>').appendTo(deleteModal);
    var deleteControl = $('<a href="#" id="delete-link-id-' + job.ID + '" data-reveal-id="delete-job-confirmation-' + job.ID + '" data-reveal>Delete</a>');
    deleteControl.data("modalId", "#delete-job-confirmation-" + job.ID);
    deleteControl.click(function(e) {
        e.preventDefault();
        $($(this).data("modalId")).foundation('reveal', 'open');
    });
    doDeleteButton.data("url", job.url_delete);
    doDeleteButton.click(function(e) {
        e.preventDefault();
        $('a.close-reveal-modal').trigger('click');
        $.getJSON($(this).data("url"), function(data) {
            console.log(data);
            getJobs();
        });
    });
    if (job.Status === "running" || job.Status === "failed") {
        controls.append(pauseControl);
        controls.append("<br><br>");
        controls.append(cancelControl);
    } else if (job.Status === "paused") {
        controls.append(resumeControl);
        controls.append("<br><br>");
        controls.append(cancelControl);
    } else {
        controls.append(deleteControl);
    }

    return single_job_row;
}

function pollJobStatus() {
    if (jobPollingOn) {
        $.ajax({
            url: BASE_URL + '/jobs/' + tracking_job_id,
            dataType: 'json',
            type: 'GET',
            contentType: "application/json; charset=utf-8",
            error: function() {
                console.log("ERROR: failed to fetch job data");
                $("#statusContainer").text("ERROR: failed to fetch job data");
                jobPollingOn = false;
                $('#pollingSwitch').text('start polling');
            },
            success: function(data) {
                console.log(data);
                jobStatus = data.response.job.Status;
                $("#statusText").text("Job status: " + jobStatus);
                var job = data.response.job;
                var jobId = job.ID;
                var single_job_row = $('#' + jobId);
                if (single_job_row.length < 1) {
                    single_job_row = $('<div class="row" id="' + jobId + '"></div>');
                    $("#jobs_container_div").append(single_job_row);
                }

                single_job_row.empty();
                buildJobDetailsTableRow(single_job_row, job);
//                single_job_row.append('<div class="one column"><p>' + jobId + '</p></div>');
//                single_job_row.append('<div class="five column"><p>' + job.PluginName +
//                    ' [<a target="_blank" href="' + job.data_url + '">raw data</a>]' +
//                    ' [<a target="_blank" href="/SAD/visualise/' + job.PluginName + '/data.html?jobid=' + job.ID + '&num_results=20">pretty data</a>]' +
//                    ' [<a target="_blank" href="' + job.status_url + '">details</a>]' +
//                    '</p></div>');
//                single_job_row.append('<div class="one column"><p>' + job.Executions_num + '</p></div>');
//                single_job_row.append('<div class="one column"><p>' + job.Status + '</p></div>');
//                single_job_row.append('<div class="two column"><p>' + job.WhenCreated_as_string + '</p></div>');
//                single_job_row.append('<div class="two column"><p>' + job.WhenLastrun_as_string + '</p></div>');

                if (jobPollingOn) {
                    if (jobStatus === "finished" || jobStatus === "failed") {
                        jobPollingOn = false;
                        console.log("Job execution finished or failed, polling stopped");
                    } else {
                        setTimeout(pollJobStatus, pollingIntervalInSeconds * 1000);
                    }
                }

            }
        });
    }
}

function syntaxHighlight(json) {
    if (typeof json !== 'string') {
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
