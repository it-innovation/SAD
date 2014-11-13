var user = "N/A";
var player;
var BASE_URL = "/SAD/service";
// var video_id = "56335468";
var video_id = "41038796";
var video_url = "http://vimeo.com/" + video_id;
var video_player_url = "http://player.vimeo.com/video/" + video_id;

$(document).ready(function() {

    $.ajaxSetup({ cache: true });
    $("#login_area").append("Checking Facebook login status...");
    $.getScript('//connect.facebook.net/en_UK/all.js', function(){
        FB.init({
          appId: '450961131634602',
          channelUrl: '//localhost:8081/SAD/prov.html'
        });
        FB.getLoginStatus(function(response){
            console.log(response);
            $("#login_area").empty();
            if (response.status != "connected") {
              var loginButton = $('<a href="#">Login with Facebook</a>');
              loginButton.click(function(e){
                 e.preventDefault();
                 FB.login(function(response){
                     handleLogin(response);
                 });
              });
              $("#login_area").append(loginButton);
            } else {
                handleLogin(response);
            }
        });
    });
});

function handleLogin(response) {
    console.log(response);
    $("#login_area").empty();
     FB.api('/me', function(me_response) {
         console.log(me_response);
        $("#login_area").append('<p class="no_padding">Welcome to the experiment, ' + me_response.name + '.</p>');
        user = me_response;
        var logoutButton = $('<a href="#">Logout</a>');
        logoutButton.click(function(e){
            e.preventDefault();
            FB.logout(function(logout_response){
                console.log(logout_response);
                $("#content_area").empty();
                $("#login_area").empty();
                var loginButton = $('<a href="#">Login with Facebook</a>');
                loginButton.click(function(e){
                    e.preventDefault();
                    FB.login(function(response){
                        handleLogin(response);
                    });
                });
                $("#login_area").append(loginButton);
            });
        });
        $("#login_area").append(logoutButton);

        $("#content_area").append('<h1>Welcome to Schladming!' + '</h1>');
        $("#content_area").append('<iframe id="player" src="' + video_player_url + '?api=1&amp;player_id=player" width="100%" height="530" frameborder="0" webkitallowfullscreen allowfullscreen></iframe>');
        player = $f($("#player")[0]);

        player.addEvent('ready', function(){
            player.addEvent('play', onPlay);
            player.addEvent('pause', onPause);
            player.api('getVideoWidth', function (widthValue, player_id) {
                player.api('getVideoHeight', function (heightValue, player_id) {
                    console.log(widthValue + 'x' + heightValue);
                });
            });
        });

        // report player loaded prov data
        $.ajax({
            url: BASE_URL + '/prov/player',
            dataType: 'json',
            type: 'POST',
            data: JSON.stringify({video_url: video_url, user_id: user.id, user_name: user.name, timestamp: new Date().getTime()}),
            contentType: "application/json; charset=utf-8",
            error: function(){
                console.log("Error reporting prov player loaded event");
            },
            success: function(data) {
                console.log("PROV player loaded report success");
            }
        });

     });
}

function onPlay(id) {
    console.log("PROV event: video started playing by " + user.name + " (ID: " + user.id + ") - " + id);

    player = $f($('#' + id)[0]);
    var x = '10', y = '10';
    player.api('getVideoWidth', function (widthValue, player_id) {
        player.api('getVideoHeight', function (heightValue, player_id) {
            console.log('Play with: ' + widthValue + 'x' + heightValue);
            x = widthValue;
            y = heightValue;

            $.ajax({
                url: BASE_URL + '/prov/video',
                dataType: 'json',
                type: 'POST',
                data: JSON.stringify({res: x + 'x' + y, video_url: video_url, action: "play", user_id: user.id, user_name: user.name, timestamp: new Date().getTime()}),
                contentType: "application/json; charset=utf-8",
                error: function(){
                    console.log("Error reporting prov");
                },
                success: function(data) {
                    console.log("PROV play report success");
                }
            });

        });
    });


}

function onPause(id) {
    player.api('getCurrentTime', function (value, id) {
        console.log("PROV event: video paused at: " + value + " by " + user.name + " (ID: " + user.id + ")");

        $.ajax({
            url: BASE_URL + '/prov/video',
            dataType: 'json',
            type: 'POST',
            data: JSON.stringify({video_url: video_url, action: "pause", user_id: user.id, user_name: user.name, pause_time: value, timestamp: new Date().getTime()}),
            contentType: "application/json; charset=utf-8",
            error: function(){
                console.log("Error reporting prov");
            },
            success: function(data) {
                console.log("PROV play report success");
            }
        });
    });
}