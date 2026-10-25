function calendarText(text) {
    return text.replace(/[,;\\]/g, function (c) {
        return '\\' + c;
    });
}

function calendarFile(p, date, title) {
    const day = date.split('-').join('');
    const parts = date.split('-');
    const next = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]) + 1);
    const end = next.getFullYear() + twoDigits(next.getMonth() + 1) + twoDigits(next.getDate());
    const stamp = new Date().toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';

    const lines = [
        'BEGIN:VCALENDAR',
        'VERSION:2.0',
        'PRODID:-//ReachNYC//EN',
        'BEGIN:VEVENT',
        'UID:' + p.id + '-' + day + '@reachnyc',
        'DTSTAMP:' + stamp,
        'DTSTART;VALUE=DATE:' + day,
        'DTEND;VALUE=DATE:' + end,
        'SUMMARY:' + calendarText(title + ': ' + p.name),
        'DESCRIPTION:' + calendarText(p.url),
        'URL:' + p.url,
        'BEGIN:VALARM',
        'TRIGGER:-P7D',
        'ACTION:DISPLAY',
        'DESCRIPTION:' + calendarText(t.calOneWeek(p.name)),
        'END:VALARM',
        'END:VEVENT',
        'END:VCALENDAR',
    ];
    const file = new Blob([lines.join('\r\n')], { type: 'text/calendar' });
    return URL.createObjectURL(file);
}

function isUpcoming(date) {
    return date && date.length === 10 && new Date(date + 'T23:59:59') >= TODAY;
}

function setUpCalendar(card, p) {
    const link = card.querySelector('.cal');
    const app = p.application || {};
    let kind, date, label, title;
    if (isUpcoming(app.deadline)) {
        kind = 'deadline';
        date = app.deadline;
        label = t.calDeadline;
        title = t.calApplyToday;
    } else if (isUpcoming(app.opens)) {
        kind = 'opens';
        date = app.opens;
        label = t.calOpens;
        title = t.calOpensToday;
    } else {
        return;
    }
    link.hidden = false;
    link.textContent = label;
    link.download = p.id + '-' + kind + '.ics';
    link.href = '#';
    link.addEventListener('click', function () {
        link.href = calendarFile(p, date, title);
    });
}
