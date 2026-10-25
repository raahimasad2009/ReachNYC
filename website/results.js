function searchSummary(p) {
    const bits = [];
    if (p.home) bits.push(p.isMe ? t.summaryNearYou : t.summaryNear(p.placeText));
    if (p.grade != null) bits.push(t.summaryGrade(p.grade));
    if (p.interests.length > 0) {
        const names = [];
        for (const tag of p.interests) names.push(t.interests[tag]);
        bits.push(t.summaryInto(t.list(names)));
    }
    let text = bits.length > 0 ? t.summary(bits) : t.summaryAll;
    if (!p.home) text += t.summaryTip;
    return text;
}

function rank(profile) {
    return rankPrograms(translatedPrograms(), profile, { today: TODAY, travel: travel, lang: lang });
}

function showHeader(title, sub, isList) {
    document.getElementById('start').hidden = true;
    document.getElementById('results-head').hidden = false;
    document.getElementById('list-bar').hidden = !isList;
    document.getElementById('sort').hidden = isList;
    document.getElementById('results-title').textContent = title;
    document.getElementById('results-sub').textContent = sub;
}

function groupFor(r, profile) {
    const group = groupOf(r);
    if (group !== 'near' || profile.interests.length === 0) return group;
    for (const tag of r.program.interests) {
        if (profile.interests.includes(tag)) return 'near';
    }
    return 'nearOther';
}

function showResults() {
    const profile = readProfile();
    saveProfile(profile);
    const ranked = rank(profile);
    const matches = ranked.matches;
    const hidden = ranked.hidden;
    showHeader(t.found(matches.length), searchSummary(profile), false);
    document.getElementById('status').textContent = t.foundStatus(matches.length);

    const groups = [
        { key: 'near', title: profile.home ? t.groupNear : t.groupNearNoPlace },
        { key: 'nearOther', title: profile.home ? t.groupOther : t.groupOtherNoPlace },
        { key: 'online', title: t.groupOnline },
        { key: 'varies', title: t.groupVaries, note: t.groupVariesNote },
    ];

    const wrap = document.getElementById('groups');
    wrap.innerHTML = '';
    for (const group of groups) {
        const items = [];
        for (const r of matches) {
            if (groupFor(r, profile) === group.key) items.push(r);
        }
        if (items.length === 0) continue;
        const section = make('section', 'group');
        section.append(make('h3', 'group-title', group.title));
        if (group.note) section.append(make('p', 'group-note', group.note));
        const list = make('ol', 'programs');
        for (const r of items) list.append(makeCard(r, profile));
        section.append(list);
        wrap.append(section);
    }
    if (matches.length === 0) wrap.append(make('p', 'empty', t.noneFit));

    document.getElementById('hidden-wrap').hidden = hidden.length === 0;
    document.getElementById('hidden-summary').textContent = t.hiddenSummary(hidden.length);
    const hiddenList = document.getElementById('hidden-list');
    hiddenList.innerHTML = '';
    for (const r of hidden) {
        const li = make('li', null, r.program.name + ': ');
        li.append(make('span', 'why', r.blockers.join('; ')));
        hiddenList.append(li);
    }
}

function refresh() {
    if (showingList) showList();
    else if (searched) showResults();
}

function scrollToResults() {
    document.getElementById('results').scrollIntoView({ behavior: 'smooth' });
}

function search(event) {
    event.preventDefault();
    const button = document.getElementById('find');
    button.setAttribute('aria-busy', 'true');
    button.textContent = t.searching;
    setTimeout(function () {
        showingList = false;
        document.getElementById('mylist').setAttribute('aria-pressed', 'false');
        showResults();
        searched = true;
        button.removeAttribute('aria-busy');
        button.textContent = t.find;
        button.classList.remove('changed');
        if (matchMedia('(max-width: 919px)').matches) scrollToResults();
        document.getElementById('results').focus({ preventScroll: true });
    }, 350);
}

function showTestimonials() {
    document.getElementById('voices').hidden = testimonials.length === 0;
    const quotes = document.getElementById('quotes');
    quotes.innerHTML = '';
    for (const q of testimonials) {
        const li = make('li', 'quote');
        const text = q['quote_' + lang] || q.quote;
        li.append(make('p', 'quote-text', '“' + text + '”'), make('p', 'quote-who', q.who));
        quotes.append(li);
    }
}

function feedDate(text) {
    return text.slice(0, 4) + '-' + text.slice(4, 6) + '-' + text.slice(6, 8);
}

function showFootnote() {
    const foot = document.getElementById('footnote');
    foot.innerHTML = '';
    foot.append(make('p', null, t.footChecked(formatDate(meta.last_updated, lang))));
    foot.append(make('p', null, t.footDistance));
    if (travel) {
        const start = formatDate(feedDate(travel.feed.feed_start_date), lang);
        const end = formatDate(feedDate(travel.feed.feed_end_date), lang);
        foot.append(make('p', null, t.footSubway(start, end, travel.assumptions.departure_window)));
    }
}
