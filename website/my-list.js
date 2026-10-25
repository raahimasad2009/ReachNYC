function isSaved(id) {
    return saved.includes(id);
}

function setUpSave(card, p) {
    const button = card.querySelector('.save');

    function paint() {
        const on = isSaved(p.id);
        button.textContent = on ? t.saved : t.save;
        button.setAttribute('aria-pressed', String(on));
        button.setAttribute('aria-label', on ? t.unsaveLabel(p.name) : t.saveLabel(p.name));
    }

    paint();
    button.addEventListener('click', function () {
        if (isSaved(p.id)) saved.splice(saved.indexOf(p.id), 1);
        else saved.push(p.id);
        store(SAVED_KEY, saved);
        document.getElementById('mylist').textContent = t.myList(saved.length);
        paint();
        if (showingList) showList();
    });
}

function showList() {
    const profile = readProfile();
    const ranked = rank(profile);
    const all = ranked.matches.concat(ranked.hidden);
    const mine = [];
    for (const r of all) {
        if (isSaved(r.program.id)) mine.push(r);
    }
    showHeader(t.listTitle, mine.length > 0 ? t.listCount(mine.length) : '', true);
    document.getElementById('hidden-wrap').hidden = true;

    const wrap = document.getElementById('groups');
    wrap.innerHTML = '';
    if (mine.length === 0) {
        wrap.append(make('p', 'empty', t.listEmpty));
        return;
    }
    const list = make('ol', 'programs');
    for (const r of mine) list.append(makeCard(r, profile));
    wrap.append(list);
}

function toggleList() {
    showingList = !showingList;
    document.getElementById('mylist').setAttribute('aria-pressed', String(showingList));
    if (showingList) {
        showList();
        scrollToResults();
    } else if (searched) {
        showResults();
    } else {
        document.getElementById('results-head').hidden = true;
        document.getElementById('list-bar').hidden = true;
        document.getElementById('groups').innerHTML = '';
        document.getElementById('start').hidden = false;
    }
}
