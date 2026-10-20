var urlInput = document.getElementById('urlInput');
var shortcutsBar = document.getElementById('shortcutsBar');

document.getElementById('brandBtn').onclick = goHome;
document.getElementById('goBtn').onclick = loadUserUrl;
document.getElementById('addBtn').onclick = addBookmark;
document.getElementById('ytBtn').onclick = function() { openBookmarkUrl('https://m.youtube.com'); };
document.getElementById('ytMusicBtn').onclick = function() { openBookmarkUrl('https://music.youtube.com'); };
document.getElementById('lyricsBtn').onclick = function() { openBookmarkUrl('player.html'); }; // <-- Added Lyrics Page Link

document.getElementById('backBtn').onclick = function() { history.back(); };
document.getElementById('forwardBtn').onclick = function() { history.forward(); };
document.getElementById('homeBtn').onclick = goHome;
document.getElementById('refreshBtn').onclick = function() { location.reload(); };

urlInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        loadUserUrl();
    }
});

// Live Clock Engine
function updateClock() {
    var now = new Date();
    var hours = now.getHours();
    var minutes = now.getMinutes().toString();
    if (minutes.length < 2) minutes = '0' + minutes;
    var ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12 || 12;

    document.getElementById('timeDisplay').innerText = hours + ':' + minutes + ' ' + ampm;
    
    var options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    document.getElementById('dateDisplay').innerText = now.toLocaleDateString(undefined, options);
}
setInterval(updateClock, 1000);
updateClock();

// Dynamic Location & Weather (Fully Dynamic via IP/GPS)
function fetchWeather() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position) {
            getWeatherData(position.coords.latitude, position.coords.longitude, "Current Location");
        }, function() {
            fetchIpLocation();
        });
    } else {
        fetchIpLocation();
    }
}

function fetchIpLocation() {
    fetch('https://ipapi.co/json/')
        .then(function(res) { return res.json(); })
        .then(function(ipData) {
            if (ipData && ipData.latitude && ipData.longitude) {
                var cityLabel = ipData.city || ipData.region || "Current Region";
                getWeatherData(ipData.latitude, ipData.longitude, cityLabel);
            } else {
                document.getElementById('weatherCondition').innerText = "Location unavailable";
            }
        })
        .catch(function() {
            document.getElementById('weatherCondition').innerText = "Weather unavailable";
        });
}

function getWeatherData(lat, lon, locationLabel) {
    fetch('https://api.open-meteo.com/v1/forecast?latitude=' + lat + '&longitude=' + lon + '&current_weather=true')
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data && data.current_weather) {
                var temp = Math.round(data.current_weather.temperature);
                var code = data.current_weather.weathercode;
                document.getElementById('weatherTemp').innerText = temp + '°C';
                document.getElementById('weatherCondition').innerText = getWeatherDescription(code);
                document.getElementById('weatherLocation').innerText = locationLabel;
            }
        })
        .catch(function() {
            document.getElementById('weatherCondition').innerText = "Weather unavailable";
        });
}

function getWeatherDescription(code) {
    var weatherCodes = {
        0: "Clear sky", 1: "Mainly clear", 2: "Partly cloudy", 3: "Overcast",
        45: "Foggy", 51: "Light drizzle", 61: "Slight rain", 63: "Moderate rain",
        65: "Heavy rain", 95: "Thunderstorm"
    };
    return weatherCodes[code] || "Fair";
}
fetchWeather();

// Dynamic Bookmark Management
function getBookmarks() {
    var stored = localStorage.getItem('autodeck_bookmarks');
    if (stored) {
        try { return JSON.parse(stored); } catch (e) {}
    }
    return [];
}

function saveBookmarks(bookmarks) {
    localStorage.setItem('autodeck_bookmarks', JSON.stringify(bookmarks));
}

function renderBookmarks() {
    var bookmarks = getBookmarks();
    shortcutsBar.innerHTML = '';

    if (bookmarks.length === 0) {
        shortcutsBar.innerHTML = '<span style="color: #666; font-size: 11px; padding: 2px;">No bookmarks. Type URL & tap "+"</span>';
        return;
    }

    bookmarks.forEach(function(item, index) {
        var chip = document.createElement('button');
        chip.className = 'chip';
        chip.innerHTML = '<span>' + item.name + '</span>';
        chip.onclick = function() { openBookmarkUrl(item.url); };

        var delBtn = document.createElement('span');
        delBtn.className = 'delete-shortcut';
        delBtn.innerHTML = '✕';
        delBtn.onclick = function(e) {
            e.stopPropagation();
            deleteBookmark(index);
        };
        chip.appendChild(delBtn);

        shortcutsBar.appendChild(chip);
    });
}

function formatUrl(rawVal) {
    var url = rawVal.trim();
    if (!url) return '';
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        url = 'https://' + url;
    }
    return url;
}

function loadUserUrl() {
    var rawVal = urlInput.value;
    if (!rawVal) return;
    window.location.href = formatUrl(rawVal);
}

function addBookmark() {
    var rawVal = urlInput.value.trim();
    if (!rawVal) {
        alert("Please enter a URL in the address bar first.");
        return;
    }

    var targetUrl = formatUrl(rawVal);
    var defaultTitle = rawVal.replace('https://', '').replace('http://', '');
    var name = prompt("Enter bookmark title:", defaultTitle);
    if (!name) return;

    var bookmarks = getBookmarks();
    bookmarks.push({ name: name, url: targetUrl });
    saveBookmarks(bookmarks);
    renderBookmarks();
}

function deleteBookmark(index) {
    var bookmarks = getBookmarks();
    bookmarks.splice(index, 1);
    saveBookmarks(bookmarks);
    renderBookmarks();
}

function openBookmarkUrl(url) {
    window.location.href = url;
}

function goHome() {
    window.location.href = "timonauto.html";
}

renderBookmarks();
