const TODAY = new Date();
const PROFILE_KEY = 'reachnyc-profile';
const SAVED_KEY = 'reachnyc-saved';
const LANG_KEY = 'reachnyc-lang';

const BULLET_NAMES = { FX: 'F', '6X': '6', '7X': '7', GS: 'S', FS: 'S', H: 'S', SI: 'SIR' };
const CHIP_COLORS = ['#ffd23f', '#2bc48a', '#ff8fbf', '#b9a8ff', '#7fd1ff'];

let lang = 'en';
let t = messages('en');
let programs = [];
let translations = {};
let meta = {};
let travel = null;
let testimonials = [];
let saved = [];
let searched = false;
let showingList = false;
let myLocation = null;
const places = {};
