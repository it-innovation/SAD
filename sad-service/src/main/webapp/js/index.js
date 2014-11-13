var BASE_URL = "/" + window.location.href.split('/')[3];

$(document).ready(function() {

    $(document).foundation();

    // check if the service is initialised successfully.
    $.ajax({
        type: 'GET',
        url: BASE_URL + "/configuration/ifinitialised",
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(jqXHR);
            console.log(textStatus);
            console.log(errorThrown);
            $('#configStatus').attr('class', 'alert-color');
            $('#configStatus').text("Service Initialisation Error (" + errorThrown + ")");
        },
        success: function(ifinit_data) {
            console.log(ifinit_data);
            if (ifinit_data === false) {
                $('#configStatus').attr('class', 'alert-color');
                $('#configStatus').text('Initialisation failed');
            } else {
                $('#configStatus').attr('class', 'success-color');
                $('#configStatus').text('Service Initialised');

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
                        if (ifstart_data === false) {

                            // check if configuration was set.
                            $.ajax({
                                type: 'GET',
                                url: BASE_URL + "/configuration",
                                error: function(jqXHR, textStatus, errorThrown) {
                                    console.log(jqXHR);
                                    console.log(textStatus);
                                    console.log(errorThrown);
                                    $('#configStatus').attr('class', 'alert-color');
                                    $('#configStatus').text("No configuration (" + errorThrown + ")");
                                },
                                success: function(configuration_data) {
                                    console.log(configuration_data);
                                    $("#config_basepath").val(configuration_data.basepath);
                                    $("#config_pluginsPath").val(configuration_data.pluginsPath);

                                    if (configuration_data.resetDatabaseOnStart === false) {
                                        $("#config_resetDatabaseOnStart").prop('checked', false);
                                    } else {
                                        $("#config_resetDatabaseOnStart").prop('checked', true);
                                    }
                                    $("#config_coordinatorPath").val(configuration_data.coordinatorPath);


                                    if (configuration_data.eccEnabled === false) {
                                        $("#config_useecc").prop('checked', false);
                                    } else {
                                        $("#config_useecc").prop('checked', true);
                                    }
                                    $("#config_rabbitip").val(configuration_data.ecc.rabbitIp);
                                    $("#config_rabbitport").val(configuration_data.ecc.rabbitPort);
                                    $("#config_monitorid").val(configuration_data.ecc.monitorId);
                                    $("#config_clientname").val(configuration_data.ecc.clientName);
                                    $("#config_clientuuidseed").val(configuration_data.ecc.clientsUuuidSeed);
                                }});
                        } else {
                            window.location.replace(BASE_URL + "/control.html");
                        }
                    }});
            }
        }});

    $("#configurationForm").submit(function(e) {
        e.preventDefault();
        $("#setActiveConfiguration").trigger('click');
    });

    $("#setConfiguration").click(function(e) {
        e.preventDefault();

        var newConfiguration = $("#configurationForm").serializeJSON();
        if (newConfiguration.eccEnabled === "false") {
            newConfiguration.eccEnabled = false;
        } else {
            newConfiguration.eccEnabled = true;
        }
        if (newConfiguration.resetDatabaseOnStart === "false") {
            newConfiguration.resetDatabaseOnStart = false;
        } else {
            newConfiguration.resetDatabaseOnStart = true;
        }

        console.log(newConfiguration);

        $.ajax({
            type: 'POST',
            dataType: 'json',
            contentType: 'application/json',
            url: BASE_URL + "/configuration",
            data: JSON.stringify(newConfiguration),
            error: function(jqXHR, textStatus, errorThrown) {
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
                $('#configStatus').attr('class', 'alert-color');
                $('#configStatus').text("Failed to submit new configuration (" + errorThrown + ")");
//                $("#newConfigurationErrorModal").foundation('reveal', 'open');
            },
            success: function(data) {
                console.log(data);
                if (data === false) {
                    $('#configStatus').attr('class', 'right alert-color');
                    $('#configStatus').text('Configuration failed');
//                    $("#newConfigurationErrorModal").foundation('reveal', 'open');
                } else {
                    $('#configStatus').attr('class', 'right success-color');
                    $('#configStatus').text('Service Started');

                    window.location.replace(BASE_URL + "/control.html");
                }
            }
        });
    });
});