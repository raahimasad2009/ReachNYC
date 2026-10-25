function gradeText(n) {
    return n === 0 ? 'K' : String(n);
}

function rangeText(range) {
    const low = range.min == null ? 0 : range.min;
    if (range.max == null) return gradeText(low) + '+';
    if (low === range.max) return gradeText(low);
    return gradeText(low) + '-' + gradeText(range.max);
}

function inRange(value, range) {
    if (range.min != null && value < range.min) return false;
    if (range.max != null && value > range.max) return false;
    return true;
}

// adds a blocker if the program is not for you, or a caution if we are not sure
function checkRules(program, profile, t, blockers, cautions) {
    const rules = program.eligibility || {};

    if (profile.grade != null) {
        if (program.grades) {
            if (!inRange(profile.grade, program.grades)) blockers.push(t.blockGrades(rangeText(program.grades)));
        } else if (!(profile.age != null && program.ages)) {
            cautions.push(t.cautionGradeUnknown);
        }
    }

    if (profile.age != null && program.ages && !inRange(profile.age, program.ages)) {
        blockers.push(t.blockAges(rangeText(program.ages)));
    }

    if (rules.nyc_public_school_only) {
        if (profile.schoolType === 'private' || profile.schoolType === 'homeschool') blockers.push(t.blockPublicOnly);
        if (profile.schoolType === 'charter') cautions.push(t.cautionCharter);
    }

    const seasons = program.seasons || [];
    if (profile.seasons && profile.seasons.length > 0) {
        if (seasons.length === 0) {
            cautions.push(t.cautionSeasonUnknown);
        } else {
            let fits = false;
            const names = [];
            for (const season of seasons) {
                if (profile.seasons.includes(season)) fits = true;
                names.push(t.seasons[season]);
            }
            if (!fits) blockers.push(t.blockSeason(names.join(' / ')));
        }
    }

    if (profile.formats && profile.formats.length > 0 && program.format !== 'hybrid') {
        if (program.format === 'online' && !profile.formats.includes('online')) blockers.push(t.blockOnlineOnly);
        if (program.format === 'in_person' && !profile.formats.includes('in_person')) blockers.push(t.blockInPersonOnly);
    }
}

function scoreInterests(program, profile, t, reasons) {
    const picked = profile.interests || [];
    const matching = [];
    const labels = [];
    for (const tag of program.interests) {
        if (picked.includes(tag)) {
            matching.push(tag);
            labels.push(t.interests[tag]);
        }
    }
    let score = 0.5;
    if (picked.length > 0) {
        score = Math.min(1, matching.length / Math.min(2, picked.length));
    }
    if (matching.length > 0) {
        reasons.push(t.reasonInterests(labels.join(', '), matching.length));
    }
    return score;
}

function scoreDistance(program, commute, distance, maxMinutes, t, reasons, cautions) {
    let score = 0.45;
    if (program.format === 'online') {
        score = 0.7;
        reasons.push(t.reasonOnline);
    } else if (commute) {
        score = Math.max(0, 1 - commute.minutes / maxMinutes);
        if (commute.walkAtEnd >= 15 && !commute.walkOnly) cautions.push(t.cautionLongWalk(commute.walkAtEnd));
    } else if (distance) {
        score = Math.max(0, 1 - distance.miles / 8);
    } else if (!program.locations || program.locations.length === 0) {
        cautions.push(program.location_note || t.cautionLocationVaries);
    }
    return score;
}

// stipends and college credit are a bonus
function scoreExtras(program, t, reasons) {
    let score = 0;
    if (program.stipend) {
        score += 0.6;
        if (program.stipend.amount) reasons.push(t.reasonStipendAmount(program.stipend.amount.toLocaleString('en-US')));
        else reasons.push(t.reasonStipend);
    }
    if (program.interests.includes('college_credit')) {
        score += 0.4;
        reasons.push(t.reasonCredit);
    }
    return score;
}

// lose a few points if we don't know the cost
function costPenalty(program, t, cautions) {
    const rules = program.eligibility || {};
    let penalty = 0;
    if (program.cost.free == null) {
        penalty = 3;
        cautions.push(t.cautionCostUnknown);
    } else if (program.cost.free === 'some') {
        penalty = 2;
        cautions.push(t.cautionCostSome);
    }
    if (rules.income_based) cautions.push(t.cautionIncome);
    if (rules.nyc_resident === true) cautions.push(t.cautionNycResident);
    return penalty;
}

function evaluate(program, profile, options = {}) {
    const today = options.today || new Date();
    const lang = options.lang || 'en';
    const t = messages(lang);
    const blockers = [];
    const reasons = [];
    const cautions = [];

    checkRules(program, profile, t, blockers, cautions);

    const commute = commuteFor(program, profile, options.travel);
    const distance = nearestSite(program, profile.home);
    const maxMinutes = profile.maxCommuteMin || 60;
    if (commute && commute.minutes > maxMinutes) {
        blockers.push(t.blockTooFar(commute.minutes, maxMinutes));
    }

    const interestScore = scoreInterests(program, profile, t, reasons);
    const distanceScore = scoreDistance(program, commute, distance, maxMinutes, t, reasons, cautions);
    const application = applicationState(program, today, lang);
    const extraScore = scoreExtras(program, t, reasons);
    const penalty = costPenalty(program, t, cautions);

    const parts = {
        interest: Math.round(50 * interestScore),
        commute: Math.round(25 * distanceScore),
        timing: Math.round(15 * application.score),
        value: Math.round(10 * Math.min(1, extraScore)),
    };
    let score = parts.interest + parts.commute + parts.timing + parts.value - penalty;
    score = Math.max(0, Math.min(100, score));

    return {
        program: program,
        eligible: blockers.length === 0,
        blockers: blockers,
        score: score,
        parts: parts,
        reasons: reasons,
        cautions: cautions,
        commute: commute,
        distance: distance,
        application: application,
    };
}

function groupOf(result) {
    if (result.program.format === 'online') return 'online';
    if (result.commute || result.distance) return 'near';
    return 'varies';
}

// programs with subway minutes come first, then ones we only have miles for
function closeness(result) {
    if (result.commute) return result.commute.minutes;
    if (result.distance) return 1000 + result.distance.miles;
    return 1000000;
}

function rankPrograms(programs, profile, options = {}) {
    const matches = [];
    const hidden = [];
    for (const program of programs) {
        const result = evaluate(program, profile, options);
        if (result.eligible) matches.push(result);
        else hidden.push(result);
    }

    matches.sort(function (a, b) {
        if (profile.sort === 'match' && a.score !== b.score) return b.score - a.score;
        if (closeness(a) !== closeness(b)) return closeness(a) - closeness(b);
        if (a.score !== b.score) return b.score - a.score;
        return a.program.name.localeCompare(b.program.name);
    });
    hidden.sort(function (a, b) {
        return a.program.name.localeCompare(b.program.name);
    });

    return { matches: matches, hidden: hidden };
}
