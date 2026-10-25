function parseDate(text) {
    if (!text) return null;
    const parts = text.split('-');
    const year = Number(parts[0]);
    const month = Number(parts[1]);
    const day = parts[2] ? Number(parts[2]) : 1;
    if (!year || !month) return null;
    return new Date(year, month - 1, day);
}

function formatDate(text, lang = 'en') {
    const date = parseDate(text);
    if (!date) return '';
    const options = { month: 'short', year: 'numeric' };
    if (text.split('-').length === 3) options.day = 'numeric';
    return date.toLocaleDateString(messages(lang).locale, options);
}

function daysUntil(text, today) {
    const date = parseDate(text);
    if (!date) return null;
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    return Math.round((date - start) / (24 * 60 * 60 * 1000));
}

function applicationState(program, today, lang = 'en') {
    const t = messages(lang);
    const app = program.application || { status: 'unknown' };

    if (app.deadline) {
        const days = daysUntil(app.deadline, today);
        if (days >= 0) {
            const soon = days <= 60;
            return { score: soon ? 1 : 0.8, label: t.applyBy(formatDate(app.deadline, lang)), detail: t.daysLeft(days), urgent: soon };
        }
        return { score: 0.15, label: t.lastPassed(formatDate(app.deadline, lang)), detail: t.nextCycle, urgent: false };
    }

    if (app.status === 'open') {
        return { score: 1, label: t.enrolling, detail: null, urgent: true };
    }
    if (app.status === 'rolling') {
        return { score: 0.85, label: t.rolling, detail: t.anytime, urgent: false };
    }
    if (app.status === 'opens_later') {
        const days = daysUntil(app.opens, today);
        if (days > 0) {
            return { score: days <= 120 ? 0.75 : 0.6, label: t.opensOn(formatDate(app.opens, lang)), detail: null, urgent: false };
        }
        return { score: 0.6, label: t.notPosted, detail: null, urgent: false };
    }
    if (app.status === 'closed') {
        return { score: 0.1, label: t.closed, detail: null, urgent: false };
    }
    return { score: 0.4, label: t.notListed, detail: t.checkWith, urgent: false };
}
