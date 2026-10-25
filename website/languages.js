const LANGUAGES = { en: english, es: spanish, ru: russian, ur: urdu };

function messages(lang) {
    if (LANGUAGES[lang]) return LANGUAGES[lang];
    return english;
}
