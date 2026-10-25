function start() {
    t = messages(load(LANG_KEY));
    if (typeof programData === 'undefined') {
        document.getElementById('start').textContent = t.loadFailed;
        return;
    }

    if (typeof subwayTimes !== 'undefined') travel = subwayTimes;
    translations.es = spanishPrograms;
    translations.ru = russianPrograms;
    translations.ur = urduPrograms;

    programs = programData.programs;
    meta = programData.meta;
    testimonials = testimonialList;
    saved = [];
    for (const id of load(SAVED_KEY) || []) {
        for (const p of programs) {
            if (p.id === id) saved.push(id);
        }
    }
    if (!travel) document.getElementById('ride-field').hidden = true;

    buildLanguageMenu();
    buildPlaces();
    buildGrades();
    buildInterests();
    restoreProfile();
    setLanguage(load(LANG_KEY) || 'en');

    document.getElementById('lang').addEventListener('change', function () {
        setLanguage(document.getElementById('lang').value);
    });
    document.getElementById('locate').addEventListener('click', useMyLocation);
    document.getElementById('profile').addEventListener('input', formChanged);
    document.getElementById('profile').addEventListener('change', formChanged);
    document.getElementById('profile').addEventListener('submit', search);
    document.getElementById('sort').addEventListener('change', refresh);
    document.getElementById('mylist').addEventListener('click', toggleList);
    document.getElementById('print').addEventListener('click', function () {
        window.print();
    });
}

start();
