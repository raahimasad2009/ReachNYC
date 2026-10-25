function stationLabel(station) {
    const names = [];
    for (const line of station.lines) {
        const name = BULLET_NAMES[line] || line;
        if (!names.includes(name)) names.push(name);
    }
    return station.name + ' (' + names.join(' ') + ')';
}

function buildPlaces() {
    const list = document.getElementById('place-list');
    for (const n of NEIGHBORHOODS) {
        places[n[0]] = { lat: n[1], lon: n[2] };
        list.append(new Option('', n[0]));
    }
    if (travel) {
        for (const s of travel.stations) {
            const label = stationLabel(s);
            places[label] = { lat: s.lat, lon: s.lon, stationId: s.id };
            list.append(new Option('', label));
        }
    }
}

function isMyLocation(text) {
    for (const code in LANGUAGES) {
        if (LANGUAGES[code].myLocation === text) return true;
    }
    return false;
}

function useMyLocation() {
    const hint = document.getElementById('place-hint');
    if (!navigator.geolocation) {
        hint.textContent = t.noGeo;
        return;
    }
    hint.textContent = t.findingYou;
    navigator.geolocation.getCurrentPosition(
        function (pos) {
            myLocation = { lat: pos.coords.latitude, lon: pos.coords.longitude };
            document.getElementById('place').value = t.myLocation;
            updateHints();
        },
        function () {
            hint.textContent = t.geoFailed;
        },
        { timeout: 10000, maximumAge: 600000 }
    );
}

function findHome(text) {
    const result = { home: null, homeStationId: null, walkToStationMin: 0, stationName: null, isMe: isMyLocation(text) };
    const home = result.isMe ? myLocation : places[text];
    if (!home) return result;
    result.home = home;

    if (home.stationId) {
        result.homeStationId = home.stationId;
        for (const s of travel.stations) {
            if (s.id === home.stationId) result.stationName = s.name;
        }
    } else if (travel) {
        const near = nearestStation(travel.stations, home);
        result.homeStationId = near.station.id;
        result.walkToStationMin = near.walkMinutes;
        result.stationName = near.station.name;
    }
    return result;
}

function buildGrades() {
    const select = document.getElementById('grade');
    const current = select.value;
    select.innerHTML = '';
    select.append(new Option(t.pickOne, ''));
    for (let g = 0; g <= 12; g++) {
        const label = t.grade(g);
        select.append(new Option(label.charAt(0).toUpperCase() + label.slice(1), String(g)));
    }
    select.value = current;
}

function buildInterests() {
    const wrap = document.getElementById('interests');
    const picked = checkedValues('interests');
    wrap.innerHTML = '';
    const tags = meta.interest_tags || [];
    for (let i = 0; i < tags.length; i++) {
        const tag = tags[i];
        const label = make('label', 'chip');
        label.style.setProperty('--chip', CHIP_COLORS[i % CHIP_COLORS.length]);
        const box = document.createElement('input');
        box.type = 'checkbox';
        box.value = tag;
        box.checked = picked.includes(tag);
        label.append(box, make('span', null, t.interests[tag] || tag));
        wrap.append(label);
    }
}

function readProfile() {
    const placeText = document.getElementById('place').value.trim();
    const profile = findHome(placeText);
    profile.placeText = placeText;
    profile.grade = numberOrNull(document.getElementById('grade').value);
    profile.age = numberOrNull(document.getElementById('age').value);
    profile.schoolType = document.getElementById('school').value || null;
    profile.interests = checkedValues('interests');
    profile.seasons = checkedValues('seasons');
    profile.formats = checkedValues('formats');
    profile.maxCommuteMin = Number(document.getElementById('maxCommute').value);
    profile.sort = document.querySelector('input[name="sort"]:checked').value;
    return profile;
}

function saveProfile(p) {
    store(PROFILE_KEY, {
        placeText: p.isMe ? '' : p.placeText,
        grade: p.grade,
        age: p.age,
        schoolType: p.schoolType,
        interests: p.interests,
        seasons: p.seasons,
        formats: p.formats,
        maxCommuteMin: p.maxCommuteMin,
        sort: p.sort,
    });
}

function restoreProfile() {
    const p = load(PROFILE_KEY);
    if (!p) return;
    document.getElementById('place').value = p.placeText || '';
    document.getElementById('grade').value = p.grade == null ? '' : p.grade;
    document.getElementById('age').value = p.age == null ? '' : p.age;
    document.getElementById('school').value = p.schoolType || '';
    document.getElementById('maxCommute').value = p.maxCommuteMin || 45;
    for (const id of ['interests', 'seasons', 'formats']) {
        const list = p[id] || [];
        for (const box of document.getElementById(id).querySelectorAll('input')) box.checked = list.includes(box.value);
    }
    const sort = document.querySelector('input[name="sort"][value="' + p.sort + '"]');
    if (sort) sort.checked = true;
}

function updateHints() {
    const p = readProfile();
    document.getElementById('maxCommuteOut').textContent = t.minutes(p.maxCommuteMin);
    const hint = document.getElementById('place-hint');
    if (p.placeText && !p.home) hint.textContent = t.placeHintPick;
    else if (p.home && p.stationName) hint.textContent = t.placeHintStation(p.stationName);
    else if (p.home) hint.textContent = t.placeHintOk;
    else hint.textContent = t.placeHintDefault;
}

function formChanged() {
    updateHints();
    if (searched) {
        document.getElementById('find').textContent = t.update;
        document.getElementById('find').classList.add('changed');
    }
}
