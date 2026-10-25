function bullet(line) {
    const label = BULLET_NAMES[line] || line;
    const span = make('span', 'bullet', label);
    let color = '#808183';
    let textColor = '#fff';
    if (travel && travel.lines && travel.lines[line]) {
        color = travel.lines[line].color;
        textColor = travel.lines[line].text;
    }
    span.style.background = color;
    span.style.color = textColor;
    span.setAttribute('role', 'img');
    span.setAttribute('aria-label', label + ' train');
    return span;
}

function miles(distance) {
    const bdi = document.createElement('bdi');
    bdi.textContent = formatMiles(distance.miles);
    return bdi;
}

function showTrip(card, r) {
    const main = card.querySelector('.trip-main');
    const lines = card.querySelector('.trip-lines');
    const detail = card.querySelector('.trip-detail');
    const commute = r.commute;
    const distance = r.distance;

    if (r.program.format === 'online') {
        main.textContent = t.online;
        detail.textContent = t.tripOnline;
    } else if (commute) {
        main.append(String(commute.minutes), make('small', null, t.min));
        if (commute.walkOnly) {
            detail.textContent = t.tripWalk;
        } else {
            for (const line of commute.lines) lines.append(bullet(line));
            detail.textContent = commute.transfers === 0 ? t.tripOneTrain : t.tripChanges(commute.transfers);
        }
        if (distance) detail.append(t.dir === 'rtl' ? '، ' : ', ', miles(distance));
    } else if (distance) {
        main.append(miles(distance));
        detail.textContent = t.tripAway;
    } else {
        main.textContent = '?';
        const hasSites = r.program.locations && r.program.locations.length > 0;
        detail.textContent = hasSites ? t.tripTellUs : t.tripFewPlaces;
    }
}

function addFact(list, term, value, link) {
    if (!value) return;
    const dd = make('dd');
    if (link) {
        const a = make('a', null, value);
        a.href = link;
        a.target = '_blank';
        a.rel = 'noopener';
        a.dir = 'ltr';
        dd.append(a);
    } else {
        dd.textContent = value;
    }
    list.append(make('dt', null, term), dd);
}

function addTags(card, r) {
    const p = r.program;
    const tags = card.querySelector('.tags');
    if (p.cost && p.cost.free === true) tags.append(make('span', 'tag free', t.tagFree));
    if (p.stipend) {
        const amount = p.stipend.amount ? p.stipend.amount.toLocaleString('en-US') : null;
        tags.append(make('span', 'tag', t.tagPays(amount)));
    }
    const app = r.application;
    const appText = app.detail ? app.label + ' (' + app.detail + ')' : app.label;
    tags.append(make('span', app.urgent ? 'tag soon' : 'tag', appText));
}

function addReasons(card, r) {
    const p = r.program;
    const reasons = card.querySelector('.reasons');
    for (const text of r.reasons) {
        if (text === t.reasonOnline || text === t.reasonStipend) continue;
        if (p.stipend && text.includes('$')) continue;
        reasons.append(make('li', null, text));
    }
    if (r.cautions.length > 0) {
        const li = make('li');
        li.append(make('strong', null, t.headsUp), r.cautions.map(endSentence).join(' '));
        card.querySelector('.cautions').append(li);
    }
}

// the "more info" part of the card
function addFacts(card, r, profile) {
    const p = r.program;
    const facts = card.querySelector('.facts');

    let site = null;
    if (r.commute && r.commute.site) site = r.commute.site;
    else if (r.distance) site = r.distance.site;
    else if (p.locations && p.locations.length === 1) site = p.locations[0];

    if (site) {
        addFact(facts, t.fWhere, site.name + ', ' + site.address);
        addFact(facts, t.fNote, p.location_note);
    } else {
        addFact(facts, t.fWhere, p.location_note);
    }

    if (r.commute && !r.commute.walkOnly) {
        let start = t.gettingThereFrom(profile.stationName);
        if (profile.walkToStationMin > 0) start = t.gettingThereWalk(profile.walkToStationMin, profile.stationName);
        const route = describeCommute(r.commute, lang);
        addFact(facts, t.fGettingThere, start + ' ' + route + '. ' + t.gettingThereEnd(r.commute.walkAtEnd));
    }

    addFact(facts, t.fWhen, p.schedule);
    addFact(facts, t.fWho, p.eligibility && p.eligibility.who);
    addFact(facts, t.fCost, p.cost && p.cost.note);
    addFact(facts, t.fStipend, p.stipend && p.stipend.note);
    addFact(facts, t.fApplying, p.application && p.application.note);
    addFact(facts, t.fScore, t.score(r));
    for (let i = 0; i < p.sources.length; i++) {
        const host = new URL(p.sources[i]).hostname.replace(/^www\./, '');
        addFact(facts, i === 0 ? t.fSource : '', host, p.sources[i]);
    }
    addFact(facts, t.fChecked, formatDate(p.last_verified, lang));
}

function makeCard(r, profile) {
    const card = document.getElementById('program-template').content.firstElementChild.cloneNode(true);
    const p = r.program;
    showTrip(card, r);

    card.querySelector('.name').textContent = p.name;
    card.querySelector('.provider').textContent = p.provider;
    card.querySelector('.summary').textContent = p.summary;
    const see = card.querySelector('.see');
    see.href = p.url;
    see.textContent = t.checkItOut;
    see.setAttribute('aria-label', t.checkItOutLabel(p.name));

    addTags(card, r);

    setUpSave(card, p);
    setUpCalendar(card, p);

    addReasons(card, r);

    card.querySelector('.details summary').textContent = t.moreInfo;
    addFacts(card, r, profile);
    return card;
}
