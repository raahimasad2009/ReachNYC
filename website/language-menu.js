function translatedPrograms() {
    const tr = translations[lang];
    if (!tr) return programs;
    const result = [];
    for (const p of programs) {
        const x = tr[p.id];
        if (!x) {
            result.push(p);
            continue;
        }
        const copy = Object.assign({}, p);
        if (x.summary) copy.summary = x.summary;
        if (x.schedule) copy.schedule = x.schedule;
        if (p.location_note && x.location_note) copy.location_note = x.location_note;
        copy.cost = Object.assign({}, p.cost);
        if (x.cost) copy.cost.note = x.cost;
        if (p.stipend) {
            copy.stipend = Object.assign({}, p.stipend);
            if (x.stipend) copy.stipend.note = x.stipend;
        }
        copy.application = Object.assign({}, p.application);
        if (x.application) copy.application.note = x.application;
        copy.eligibility = Object.assign({}, p.eligibility);
        if (x.who) copy.eligibility.who = x.who;
        result.push(copy);
    }
    return result;
}

function buildLanguageMenu() {
    const select = document.getElementById('lang');
    for (const code in LANGUAGES) {
        const option = new Option(LANGUAGES[code].name, code);
        option.lang = code;
        select.append(option);
    }
}

function setLanguage(code) {
    lang = LANGUAGES[code] ? code : 'en';
    t = messages(lang);
    store(LANG_KEY, lang);

    document.documentElement.lang = lang;
    document.documentElement.dir = t.dir;
    document.getElementById('lang').value = lang;
    document.getElementById('lang-label').textContent = t.languageLabel;

    for (const node of document.querySelectorAll('[data-text]')) {
        const text = t[node.dataset.text];
        if (typeof text === 'string') node.textContent = text;
    }
    for (const node of document.querySelectorAll('[data-placeholder]')) {
        node.placeholder = t[node.dataset.placeholder];
    }
    document.getElementById('sort').setAttribute('aria-label', t.sortLabel);
    document.getElementById('find').textContent = searched ? t.update : t.find;
    document.getElementById('find').classList.remove('changed');
    document.getElementById('mylist').textContent = t.myList(saved.length);
    if (isMyLocation(document.getElementById('place').value)) document.getElementById('place').value = t.myLocation;

    buildGrades();
    buildInterests();
    showFootnote();
    showTestimonials();
    updateHints();
    refresh();
}
