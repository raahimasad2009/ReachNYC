const MAX_WALK_MINUTES = 30;
const METERS_PER_MILE = 1609.34;

function metersBetween(lat1, lon1, lat2, lon2) {
    const rad = Math.PI / 180;
    const dLat = (lat2 - lat1) * rad;
    const dLon = (lon2 - lon1) * rad;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return 2 * 6371000 * Math.asin(Math.sqrt(a));
}

// streets add about 30% to a straight line, and people walk about 80 meters a minute
function walkMinutes(meters) {
    return Math.ceil((meters * 1.3) / 80);
}

function formatMiles(miles) {
    if (miles < 10) return miles.toFixed(1) + ' mi';
    return Math.round(miles) + ' mi';
}

function nearestSite(program, home) {
    if (!home || program.format === 'online' || !program.locations || program.locations.length === 0) return null;
    let best = null;
    for (let i = 0; i < program.locations.length; i++) {
        const site = program.locations[i];
        const meters = metersBetween(home.lat, home.lon, site.lat, site.lon);
        if (best === null || meters < best.meters) {
            best = { site: site, index: i, meters: meters };
        }
    }
    best.miles = best.meters / METERS_PER_MILE;
    best.walkMinutes = walkMinutes(best.meters);
    return best;
}

function nearestStation(stations, home) {
    let best = null;
    let bestMeters = Infinity;
    for (const station of stations) {
        const meters = metersBetween(home.lat, home.lon, station.lat, station.lon);
        if (meters < bestMeters) {
            best = station;
            bestMeters = meters;
        }
    }
    if (!best) return null;
    return { station: best, walkMinutes: walkMinutes(bestMeters) };
}

function commuteFor(program, profile, travel) {
    if (!travel || !profile.homeStationId || program.format === 'online') return null;
    const row = travel.times[profile.homeStationId];
    if (!row || !row[program.id]) return null;

    const trip = row[program.id];
    const lines = trip[2];
    const walkToStation = profile.walkToStationMin || 0;
    const commute = {
        minutes: trip[0] + walkToStation,
        transfers: trip[1],
        lines: lines ? lines.split(' ') : [],
        site: program.locations ? program.locations[trip[3]] : null,
        walkAtEnd: trip[4],
        walkOnly: !lines,
    };

    const near = nearestSite(program, profile.home);
    if (near && near.walkMinutes <= MAX_WALK_MINUTES && near.walkMinutes < commute.minutes) {
        return { minutes: near.walkMinutes, transfers: 0, lines: [], site: near.site, walkAtEnd: near.walkMinutes, walkOnly: true };
    }
    return commute;
}

function describeCommute(commute, lang = 'en') {
    if (!commute) return '';
    const t = messages(lang);
    if (commute.walkOnly) return t.commuteWalk(commute.minutes);
    const changes = commute.transfers === 0 ? t.noTransfers : t.transfers(commute.transfers);
    return commute.minutes + ' min, ' + changes + ', ' + commute.lines.join(' → ');
}
