function make(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
}

function store(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {}
}

function load(key) {
    try {
        return JSON.parse(localStorage.getItem(key));
    } catch (e) {
        return null;
    }
}

function numberOrNull(value) {
    if (value === '') return null;
    return Number(value);
}

function checkedValues(id) {
    const values = [];
    for (const box of document.getElementById(id).querySelectorAll('input:checked')) values.push(box.value);
    return values;
}

function twoDigits(n) {
    return n < 10 ? '0' + n : String(n);
}

function endSentence(text) {
    if (/[.!?۔]$/.test(text)) return text;
    return text + (t.dir === 'rtl' ? '۔' : '.');
}
