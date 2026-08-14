/* a_bogus bundle: sm3 + vm_decode + env_test + utils (参考 ylcangel/douyin_sign, Apache-2.0) */
/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 国密_SM3加密{JS实现}
 * 
 * 参考 https://blog.csdn.net/Shen_yuanjia/article/details/111879819 ，这个已经付费了
 * https://www.jianshu.com/p/06905998e4e7
 * 
 * 这个版本和dy的最像，估计是dy的程序员抄的这个算法，但算法的结果不对
 * 
 * @returns
 */
function _SM3() {
    if (!(this instanceof _SM3)) {
        return new _SM3();
    }

    this.reg = new Array(8);
    this.chunk = [];
    this.size = 0;

    this.reset();
}

_SM3.prototype.reset = function () {
    this.reg[0] = 0x7380166f;
    this.reg[1] = 0x4914b2b9;
    this.reg[2] = 0x172442d7;
    this.reg[3] = 0xda8a0600;
    this.reg[4] = 0xa96f30bc;
    this.reg[5] = 0x163138aa;
    this.reg[6] = 0xe38dee4d;
    this.reg[7] = 0xb0fb0e4e;
    this.chunk = [];
    this.size = 0;
};

/**
 * 字符串转byte数组
 */
_SM3.prototype.strToBytes = function (s) {
    var ch, st, re = [];
    for (var i = 0; i < s.length; i++) {
        ch = s.charCodeAt(i);
        st = [];
        do {
            st.push(ch & 0xFF);
            ch = ch >> 8;
        }
        while (ch);
        re = re.concat(st.reverse());
    }
    return re;
};

_SM3.prototype.write = function (msg) {
    var m = (typeof msg === 'string') ? this.strToBytes(msg) : msg;
    this.size += m.length;
    var i = 64 - this.chunk.length;
    if (m.length < i) {
        this.chunk = this.chunk.concat(m);
        return;
    }

    this.chunk = this.chunk.concat(m.slice(0, i));
    while (this.chunk.length >= 64) {
        this._compress(this.chunk);
        if (i < m.length) {
            this.chunk = m.slice(i, Math.min(i + 64, m.length));
        } else {
            this.chunk = [];
        }
        i += 64;
    }
};

/**
 * 计算hash值
 */
_SM3.prototype.sum = function (msg, enc) {
    if (msg) {
        this.reset();
        this.write(msg);
    }

    this._fill();
    for (var i = 0; i < this.chunk.length; i += 64) {
        this._compress(this.chunk.slice(i, i + 64));
    }

    var digest = null;
    if (enc == 'hex') {
        digest = "";
        for (var i = 0; i < 8; i++) {
            digest += this.reg[i].toString(16);
        }
    } else {
        var digest = new Array(32);
        for (var i = 0; i < 8; i++) {
            var h;
            h = this.reg[i];
            digest[i * 4 + 3] = (h & 0xff) >>> 0;
            h >>>= 8;
            digest[i * 4 + 2] = (h & 0xff) >>> 0;
            h >>>= 8;
            digest[i * 4 + 1] = (h & 0xff) >>> 0;
            h >>>= 8;
            digest[i * 4] = (h & 0xff) >>> 0;
        }
    }

    this.reset();
    return digest;
};

_SM3.prototype._compress = function (m) {
    if (m < 64) {
        console.error("compress error: not enough data");
        return;
    }
    var w = this._expand(m);
    var r = this.reg.slice(0);
    for (var j = 0; j < 64; j++) {
        var ss1 = this._rotl(r[0], 12) + r[4] + this._rotl(this._t(j), j)
        ss1 = (ss1 & 0xffffffff) >>> 0;
        ss1 = this._rotl(ss1, 7);

        var ss2 = (ss1 ^ this._rotl(r[0], 12)) >>> 0;
        var tt1 = this._ff(j, r[0], r[1], r[2]);
        tt1 = tt1 + r[3] + ss2 + w[j + 68];
        tt1 = (tt1 & 0xffffffff) >>> 0;
        var tt2 = this._gg(j, r[4], r[5], r[6]);
        tt2 = tt2 + r[7] + ss1 + w[j];
        tt2 = (tt2 & 0xffffffff) >>> 0;
        r[3] = r[2];
        r[2] = this._rotl(r[1], 9);
        r[1] = r[0];
        r[0] = tt1;
        r[7] = r[6]
        r[6] = this._rotl(r[5], 19);
        r[5] = r[4];
        r[4] = (tt2 ^ this._rotl(tt2, 9) ^ this._rotl(tt2, 17)) >>> 0;
    }

    for (var i = 0; i < 8; i++) {
        this.reg[i] = (this.reg[i] ^ r[i]) >>> 0;
    }
};

_SM3.prototype._fill = function () {
    var l = this.size * 8;
    var len = this.chunk.push(0x80) % 64;
    if (64 - len < 8) {
        len -= 64;
    }
    for (; len < 56; len++) {
        this.chunk.push(0x00);
    }

    for (var i = 0; i < 4; i++) {
        var hi = Math.floor(l / 0x100000000);
        this.chunk.push((hi >>> ((3 - i) * 8)) & 0xff);
    }
    for (var i = 0; i < 4; i++) {
        this.chunk.push((l >>> ((3 - i) * 8)) & 0xff);
    }
};

_SM3.prototype._expand = function (b) {
    var w = new Array(132);
    for (var i = 0; i < 16; i++) {
        w[i] = b[i * 4] << 24;
        w[i] |= b[i * 4 + 1] << 16;
        w[i] |= b[i * 4 + 2] << 8;
        w[i] |= b[i * 4 + 3];
        w[i] >>>= 0;
    }

    for (var j = 16; j < 68; j++) {
        var x;
        x = w[j - 16] ^ w[j - 9] ^ this._rotl(w[j - 3], 15);
        x = x ^ this._rotl(x, 15) ^ this._rotl(x, 23);
        w[j] = (x ^ this._rotl(w[j - 13], 7) ^ w[j - 6]) >>> 0;
    }

    for (var j = 0; j < 64; j++) {
        w[j + 68] = (w[j] ^ w[j + 4]) >>> 0;
    }

    return w;
};

_SM3.prototype._rotl = function (x, n) {
    n %= 32;
    return ((x << n) | (x >>> (32 - n))) >>> 0;
};

_SM3.prototype._t = function (j) {
    if (0 <= j && j < 16) {
        return 0x79cc4519;
    } else if (16 <= j && j < 64) {
        return 0x7a879d8a;
    } else {
        console.error("invalid j for constant Tj");
    }
};

_SM3.prototype._ff = function (j, x, y, z) {
    if (0 <= j && j < 16) {
        return (x ^ y ^ z) >>> 0;
    } else if (16 <= j && j < 64) {
        return ((x & y) | (x & z) | (y & z)) >>> 0;
    } else {
        console.error("invalid j for bool function FF");
        return 0;
    }
};

_SM3.prototype._gg = function (j, x, y, z) {
    if (0 <= j && j < 16) {
        return (x ^ y ^ z) >>> 0;
    } else if (16 <= j && j < 64) {
        return ((x & y) | (~x & z)) >>> 0;
    } else {
        console.error("invalid j for bool function GG");
        return 0;
    }
};

/**
 * 等效于Array.from目的是做浏览器兼容处理 Array.from IE浏览器不支持
 */
_SM3.prototype.toArray = function (s, f) {
    var a = [];
    for (var i = 0; i < s.length; i++) {
        var t = s[i];
        if (f) {
            t = f(t);
        }
        a.push(t);
    }
    return a;
};

/**
 * _SM3加密主函数
 * 
 * @param msg
 * @returns
 */
function sm3DigestHex(msg) {
    var _sm3 = new _SM3();
    var digest = _sm3.sum(msg);
    var hashHex = _sm3.toArray(digest, function (byte) { return ('0' + (byte & 0xFF).toString(16)).slice(-2); }).join('');
    return hashHex;
}

/**
 * _SM3加密主函数
 * 
 * @param msg
 * @returns
 */
function sm3Digest(msg) {
    var _sm3 = new _SM3();
    var digest = _sm3.sum(msg);
    if (programVersion === G_DEBUG) {
        console.log("digest:: " + digest);
    }
    return digest;
}

/**
 * _SM3加密主函数
 * dy版本 ，两次sm3
 * 
 * @param msg
 * @returns
 */
function sm3DigestTwice(msg) {
    var _sm3 = new _SM3();
    var digest = _sm3.sum(msg);

    if (programVersion === G_DEBUG) {
        console.log("digest:: " + digest);
    }
    // 这个原本没有，为dy加的
    var digest1 = _sm3.sum(digest);

    if (programVersion === G_DEBUG) {
        console.log("digest:: " + digest1);
    }

    return digest1;
}
/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @Author: AngelToms
 * 整个去混淆的vm入口和解释器
 */

// 对应下面的Z大数组，其中Z[220]是"a_bogus"
Z_ARRAY = [
  "function",
  "Symbol",
  "symbol",
  "iterator",
  "constructor",
  "prototype",
  "TypeError",
  "Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.",
  "string",
  "toString",
  "call",
  "slice",
  "Object",
  "name",
  "Map",
  "Set",
  "Array",
  "from",
  "Arguments",
  "RegExp",
  "^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$",
  "test",
  "undefined",
  "@@iterator",
  "isArray",
  "length",
  "hasOwnProperty",
  "defineProperty",
  "value",
  "asyncIterator",
  "@@asyncIterator",
  "toStringTag",
  "@@toStringTag",
  "enumerable",
  "configurable",
  "writable",
  "",
  "create",
  "_invoke",
  "normal",
  "type",
  "arg",
  "throw",
  "wrap",
  "suspendedStart",
  "suspendedYield",
  "executing",
  "completed",
  "getPrototypeOf",
  "next",
  "return",
  "forEach",
  "object",
  "__await",
  "resolve",
  "then",
  "Error",
  "Generator is already running",
  "done",
  "method",
  "delegate",
  "_sent",
  "sent",
  "dispatchException",
  "abrupt",
  "The iterator does not provide a '",
  "' method",
  "resultName",
  "nextLoc",
  "iterator result is not an object",
  "0",
  "tryLoc",
  "1",
  "catchLoc",
  "2",
  "finallyLoc",
  "3",
  "afterLoc",
  "tryEntries",
  "push",
  "completion",
  "root",
  "reset",
  "isNaN",
  " is not iterable",
  "GeneratorFunction",
  "displayName",
  "isGeneratorFunction",
  "setPrototypeOf",
  "__proto__",
  "mark",
  "awrap",
  "AsyncIterator",
  "Promise",
  "async",
  "Generator",
  "[object Generator]",
  "reverse",
  "pop",
  "keys",
  "values",
  "prev",
  "t",
  "charAt",
  "rval",
  "stop",
  "end",
  "try statement without catch or finally",
  "break",
  "continue",
  "complete",
  "finish",
  "illegal catch attempt",
  "catch",
  "delegateYield",
  "Cannot call a class as a function",
  "key",
  "toPrimitive",
  "default",
  "@@toPrimitive must return a primitive value.",
  "String",
  "Number",
  "Super expression must either be null or a function",
  "bind",
  "Reflect",
  "construct",
  "apply",
  "Derived constructors may only return object or undefined",
  "ReferenceError",
  "this hasn't been initialised - super() hasn't been called",
  "Boolean",
  "valueOf",
  "window",
  "_sdkGlueVersionMap",
  "1.0.1.19-fix.01",
  "bdmsVersion",
  "aid",
  "pageId",
  "boe",
  "ddrt",
  "include",
  "exclude",
  "paths",
  "mode",
  "delay",
  "track",
  "slU",
  "dump",
  "rpU",
  "ic",
  "inner",
  "localStorage",
  "getItem",
  "xmst",
  "XMLHttpRequest",
  "open",
  "send",
  "addEventListener",
  "/web/common",
  "https://mssdk-boe.bytedance.net",
  "https://mssdk.bytedance.com",
  "URL",
  "searchParams",
  "append",
  "ms_appid",
  "msToken",
  "JSON",
  "stringify",
  "magic",
  "version",
  "dataType",
  "strData",
  "Date",
  "now",
  "tspFromClient",
  "ulr",
  "navigator",
  "sendBeacon",
  "href",
  "withCredentials",
  "load",
  "getResponseHeader",
  "x-ms-token",
  "setItem",
  "requestAnimationFrame",
  "POST",
  "document",
  "visibilitychange",
  "msgType",
  "privacyMode",
  "timestamp",
  "wID",
  "data",
  "beMove",
  "beClick",
  "beClickEnd",
  "beKeyboard",
  "windowState",
  "gyro",
  "focus",
  "screen",
  "behavior",
  "performance",
  "Request",
  "some",
  "indexOf",
  "multipart/form-data",
  "userAgent",
  "baiduboxapp",
  "replace",
  "\\s(EasyBrowser)?[Ww]ebCore=0x[a-z0-9]{9}$",
  "AlipayClient",
  "\\sChannelId\\(\\d+\\)",
  "setRequestHeader",
  "bdmsInvokeList",
  "pathname",
  "location",
  "func",
  "args",
  "has",
  "a_bogus",
  "fetch",
  "url",
  "clone",
  "text",
  "headers",
  "get",
  "content-type",
  "search",
  "body",
  "cache",
  "credentials",
  "integrity",
  "redirect",
  "referrer",
  "referrerPolicy",
  "GET",
  "toUpperCase",
  "EventSource",
  "handleUrl",
  "setTimeout",
  "setInterval",
  "includes",
  "map",
  "/web/r/token",
  "/monitor_browser/collect/batch",
  "=",
  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",
  "s0",
  "Dkdpgh4ZKsQB80/Mfvw36XI1R25+WUAlEi7NLboqYTOPuzmFjJnryx9HVGcaStCe=",
  "s1",
  "Dkdpgh4ZKsQB80/Mfvw36XI1R25-WUAlEi7NLboqYTOPuzmFjJnryx9HVGcaStCe=",
  "s2",
  "ckdp1h4ZKsUB80/Mfvw36XIgR25+WQAlEi7NLboqYTOPuzmFjJnryx9HVGDaStCe",
  "s3",
  "Dkdpgh2ZmsQB80/MfvV36XI1R45-WUAlEixNLwoqYTOPuzKFjJnry79HbGcaStCe",
  "s4",
  "charCodeAt",
  "fromCharCode",
  "Math",
  "floor",
  "random",
  "dhzx",
  "|",
  "concat",
  "trim",
  "Chrome",
  "Firefox",
  "Safari",
  "Edge",
  "Huawei",
  "4",
  "pemrissions",
  "microphone",
  "granted",
  "getOwnPropertyDescriptor",
  "grnated",
  "onwheelx",
  "_Ax",
  "sum",
  "vendorSubs",
  "ink",
  "split",
  ".",
  "getTime",
  "1721836800000",
  "9",
  "18",
  "10",
  "19",
  "11",
  "21",
  "innerWidth",
  "innerHeight",
  "outerWidth",
  "outerHeight",
  "availWidth",
  "availHeight",
  "width",
  "sizeWidth",
  "height",
  "sizeHeight",
  "platform",
  ",",
  "5",
  "6",
  "7",
  "0X21",
  "getOwnPropertySymbols",
  "filter",
  "getOwnPropertyDescriptors",
  "defineProperties",
  "t0",
  "t1",
  "t2",
  "t3",
  "t4",
  "t5",
  "t6",
  "t7",
  "t8",
  "t9",
  "canvas",
  "extra",
  "webgl",
  "audio",
  "video",
  "math",
  "envCode",
  "ubCode",
  "ms_accid",
  "custom",
  "0.0.0.1",
  "ms_version",
  "fxgDid",
  "__msuuid__",
  "uuid",
  "collectTime",
  "t10",
  "err",
  "nWID",
  "battery",
  "plugins",
  "t11",
  "t12",
  "t13",
  "t14",
  "t15",
  "t16",
  "t17",
  "t18",
  "t19",
  "t20",
  "Move",
  "ClickStart",
  "ClickEnd",
  "items",
  "front",
  "rear",
  "trigger",
  "isEmpty",
  "isFull",
  "last",
  "getTrigger",
  "clientX",
  "x",
  "clientY",
  "y",
  "ts",
  "touches",
  "item",
  "target",
  "HTMLElement",
  "nodeName",
  "BODY",
  "HTML",
  "encodeURI",
  "innerText",
  "mousemove",
  "touchmove",
  "mousedown",
  "touchstart",
  "mouseup",
  "touchend",
  "keydown",
  "mouseover",
  "mouseout",
  "self",
  "top",
  "deviceorientation",
  "beta",
  "gamma",
  "alpha",
  "z",
  "visibilityState",
  "visible",
  "v",
  "reduce",
  "sqrt",
  "pow",
  "0.2",
  "AAC",
  "HE-AAC",
  "video/x-ms-asf",
  "audio/mp4",
  "application/vnd.ms-asf",
  "audio/x-matroska",
  "audio/aacp",
  "audio/mpeg4-generic",
  "audio/MP4A-LATM",
  "video/quicktime",
  "video/x-flv",
  "audio/3gpp",
  "audio/3gpp2",
  "audio/AMR-NB",
  "audio/AMR-WB",
  "audio/GSM",
  "audio/aac",
  "audio/basic",
  "audio/flac",
  "audio/midi",
  "audio/mpeg",
  'audio/mp4; codecs="mp4a.40.2"',
  'audio/mp4; codecs="ac-3"',
  'audio/mp4; codecs="ec-3"',
  'audio/ogg; codecs="flac"',
  'audio/ogg; codecs="vorbis"',
  'audio/ogg; codecs="opus"',
  'audio/wav; codecs="1"',
  'audio/webm; codecs="vorbis"',
  'audio/webm; codecs="opus"',
  "audio/x-aiff",
  "audio/x-mpegurl",
  "timeout",
  "excludeIOS11",
  "match",
  "OS 11.+Version\\/11.+Safari",
  "EXCLUDED",
  "OfflineAudioContext",
  "webkitOfflineAudioContext",
  "NOT_AVAILABLE",
  "createOscillator",
  "triangle",
  "frequency",
  "setValueAtTime",
  "currentTime",
  "createDynamicsCompressor",
  "threshold",
  "knee",
  "ratio",
  "reduction",
  "attack",
  "release",
  "0.25",
  "connect",
  "destination",
  "start",
  "startRendering",
  "audioTimeout",
  "clearTimeout",
  "renderedBuffer",
  "getChannelData",
  "abs",
  "disconnect",
  "oncomplete",
  "audioFp",
  "createElement",
  ":",
  "canPlayType",
  "remove",
  "audioFormats",
  "AudioContext",
  "webkitAudioContext",
  "channelCount",
  "channelCountMode",
  "channelInterpretation",
  "maxChannelCount",
  "numberOfInputs",
  "numberOfOutputs",
  "sampleRate",
  "state",
  "createAnalyser",
  "fftSize",
  "frequencyBinCount",
  "maxDecibels",
  "minDecibels",
  "smoothingTimeConstant",
  "close",
  "audioContext",
  "frequencyAnalyser",
  "getBattery",
  "charging",
  "chargingTime",
  "dischargingTime",
  "round",
  "level",
  "[object Boolean]",
  "[object Function]",
  "[object Undefined]",
  "[object Number]",
  "[object String]",
  "[object Array]",
  "[object Object]",
  "[object HTMLAllCollection]",
  "[object Storage]",
  "all",
  "characterSet",
  "compatMode",
  "documentMode",
  "images",
  "layers",
  "getContext",
  "2d",
  "style",
  "inline",
  "display",
  "rect",
  "isPointInPath",
  "evenodd",
  "yes",
  "no",
  "canvasWinding",
  "alphabetic",
  "textBaseline",
  "#f60",
  "fillStyle",
  "fillRect",
  "#069",
  "11pt no-real-font-123",
  "font",
  "fillText",
  "Cwm fjordbank glyphs vext quiz, 😃",
  "rgba(102, 204, 0, 0.2)",
  "18pt Arial",
  "multiply",
  "globalCompositeOperation",
  "rgb(255,0,255)",
  "beginPath",
  "arc",
  "PI",
  "closePath",
  "fill",
  "rgb(0,255,255)",
  "rgb(255,255,0)",
  "toDataURL",
  "crc32",
  "isCanvasSupported",
  "number",
  "s",
  "n",
  "e",
  "f",
  "Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.",
  "opr",
  "InstallTrigger",
  "chrome",
  "edgeNurturingPrivate",
  "ApplePaySession",
  "72px Trebuchet MS",
  "72px Wingdings",
  "72px Sylfaen",
  "72px Segoe UI",
  "72px Constantia",
  "72px SimSun-ExtB",
  "72px MT Extra",
  "72px Gulim",
  "72px Leelawadee",
  "72px Tunga",
  "72px Meiryo",
  "72px Vrinda",
  "72px CordiaUPC",
  "72px Aparajita",
  "72px IrisUPC",
  "72px Palatino",
  "72px Colonna MT",
  "72px Playbill",
  "72px Jokerman",
  "72px Parchment",
  "72px MS Outlook",
  "72px Tw Cen MT",
  "72px OPTIMA",
  "72px Futura",
  "72px AVENIR",
  "72px Arial Hebrew",
  "72px Savoye LET",
  "72px Castellar",
  "72px MYRIAD PRO",
  "fonts",
  "check",
  "1.5",
  "Image",
  "drawImage",
  "getImageData",
  "onload",
  "onerror",
  "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
  "src",
  "bdms",
  "removeItem",
  "sessionStorage",
  "geolocation",
  "notifications",
  "midi",
  "camera",
  "speaker",
  "device-info",
  "background-sync",
  "bluetooth",
  "persistent-storage",
  "ambient-light-sensor",
  "accelerometer",
  "gyroscope",
  "magnetometer",
  "clipboard",
  "accessibility-events",
  "clipboard-read",
  "clipboard-write",
  "payment-handler",
  "permissions",
  "query",
  "denied",
  "prompt",
  "message",
  "is not a valid enum value of type PermissionName",
  "join",
  "eval",
  "indexedDB",
  "Less",
  "None",
  "More",
  "ForcedColors",
  "no-preference",
  "high",
  "more",
  "low",
  "less",
  "forced",
  "active",
  "none",
  "matchMedia",
  "(prefers-reduced-motion: ",
  ")",
  "matches",
  "(forced-colors: ",
  "(prefers-contrast: ",
  "(min-monochrome: 0)",
  "browser",
  "(max-monochrome: ",
  "no-er-mono",
  "no-mono",
  "Float32Array",
  "Uint8Array",
  "buffer",
  "rec2020",
  "p3",
  "srgb",
  "(color-gamut: ",
  "getTimezoneOffset",
  "browserType",
  "jsFontsList",
  "jsv",
  "nap",
  "nativeLength",
  "timezone",
  "useOfSessionStorage",
  "useOfLocalStorage",
  "useOfIndexedDB",
  "forcedColors",
  "monochrome",
  "openDatabase",
  "reducedMotion",
  "architecture",
  "contrast",
  "colorGamut",
  "createEvent",
  "TouchEvent",
  "ontouchstart",
  "ownKeys",
  "filename",
  "suffixes",
  "appCodeName",
  "appMinorVersion",
  "appName",
  "appVersion",
  "buildID",
  "cookieEnabled",
  "cpuClass",
  "deviceMemory",
  "doNotTrack",
  "hardwareConcurrency",
  "language",
  "languages",
  "maxTouchPoints",
  "msDoNotTrack",
  "oscpu",
  "product",
  "productSub",
  "requestMediaKeySystemAccess",
  "storage",
  "systemLanguage",
  "touchEvent",
  "userLanguage",
  "vendor",
  "vendorSub",
  "vibrate",
  "webdriver",
  "navToString",
  "acos",
  "acosh",
  "asin",
  "asinh",
  "atanh",
  "atan",
  "sin",
  "sinh",
  "cos",
  "cosh",
  "tan",
  "tanh",
  "exp",
  "expm1",
  "log1p",
  "log",
  "0.12312423423423424",
  "1e+308",
  "1e+154",
  "acoshPf",
  "asinhPf",
  "0.5",
  "atanhPf",
  "1e+300",
  "sinhPf",
  "10.000000000123",
  "coshPf",
  "tanhPf",
  "expm1Pf",
  "log1pPf",
  "powPI",
  "screenX",
  "screenY",
  "pageYOffset",
  "pageXOffset",
  "clientWidth",
  "clientHeight",
  "colorDepth",
  "pixelDepth",
  "orientation",
  "orientaionType",
  "angle",
  "orientaionAngle",
  "availTop",
  "availLeft",
  'video/mp4; codecs="flac"',
  'video/mp4; codecs="H.264, mp3"',
  'video/mp4; codecs="H.264, aac"',
  'video/mpeg; codec="H.264"',
  'video/ogg; codecs="theora"',
  'video/ogg; codecs="opus"',
  'video/webm; codecs="vp9, opus"',
  'video/webm; codecs="vp8, vorbis"',
  "videoFormats",
  "plugin",
  "pv",
  "getContextAttributes",
  "antialias",
  "getParameter",
  "BLUE_BITS",
  "blueBits",
  "DEPTH_BITS",
  "depthBits",
  "GREEN_BITS",
  "greenBits",
  "MAX_COMBINED_TEXTURE_IMAGE_UNITS",
  "maxCombinedTextureImageUnits",
  "MAX_CUBE_MAP_TEXTURE_SIZE",
  "maxCubeMapTextureSize",
  "MAX_FRAGMENT_UNIFORM_VECTORS",
  "maxFragmentUniformVectors",
  "MAX_RENDERBUFFER_SIZE",
  "maxRenderbufferSize",
  "MAX_TEXTURE_IMAGE_UNITS",
  "maxTextureImageUnits",
  "MAX_TEXTURE_SIZE",
  "maxTextureSize",
  "MAX_VARYING_VECTORS",
  "maxVaryingVectors",
  "MAX_VERTEX_ATTRIBS",
  "maxVertexAttribs",
  "MAX_VERTEX_TEXTURE_IMAGE_UNITS",
  "maxVertexTextureImageUnits",
  "MAX_VERTEX_UNIFORM_VECTORS",
  "maxVertexUniformVectors",
  "SHADING_LANGUAGE_VERSION",
  "shadingLanguageVersion",
  "STENCIL_BITS",
  "stencilBits",
  "VERSION",
  "getExtension",
  "EXT_texture_filter_anisotropic",
  "MAX_TEXTURE_MAX_ANISOTROPY_EXT",
  "maxAnisotropy",
  "WEBGL_debug_renderer_info",
  "UNMASKED_RENDERER_WEBGL",
  "renderer",
  "UNMASKED_VENDOR_WEBGL",
  "toLocaleUpperCase",
  "data:image/png;base64,",
  "atob",
  "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx",
  "[xy]",
  "g",
  "webviewBridge",
  "parent",
  "callBrowserWindow",
  "getClientInfo",
  "deviceId",
  "ActiveXObject",
  "BluetoothUUID",
  "devicePixelRatio",
  "external",
  "indexDB",
  "isSecureContext",
  "locationbar",
  "mozRTCPeerConnection",
  "netscape",
  "postMessage",
  "toolbar",
  "webkitRequestAnimationFrame",
  "code",
  "ab",
  "btoa",
  "ba",
  "alert",
  "al",
  "Function",
  "alFunc",
  "console",
  "memory",
  "me",
  "mept",
  "MouseEvent",
  "click",
  "isTrusted",
  "isT",
  "aa",
  "bb",
  "cc",
  "WebId",
  "(huawei)browser\\/([\\w.]+)",
  "i",
  "regs",
  "(chrome)\\/v?([\\w.]+)",
  "\\b(?:crmo|crios)\\/([\\w.]+)",
  "headlesschrome(?:\\/([\\w.]+)| )",
  " wv\\).+(chrome)\\/([\\w.]+)",
  "edg(?:e|ios|a)?\\/([\\w.]+)",
  "\\bfocus\\/([\\w.]+)",
  "fxios\\/([-\\w.]+)",
  "mobile vr; rv:([\\w.]+)\\).+firefox",
  "(firefox)\\/([\\w.]+)",
  "IE",
  "(?:ms|\\()(ie) ([\\w.]+)",
  "trident.+rv[: ]([\\w.]{1,9})\\b.+like gecko",
  "(iemobile)(?:browser)?[/ ]?([\\w.]*)",
  "Opera",
  "(opera mini)\\/([-\\w.]+)",
  "(opera [mobiletab]{3,6})\\b.+version\\/([-\\w.]+)",
  "(opera)(?:.+version\\/|[/ ]+)([\\w.]+)",
  "opios[/ ]+([\\w.]+)",
  "\\bopr\\/([\\w.]+)",
  "version\\/([\\w.,]+) .*mobile\\/\\w+ (safari)",
  "version\\/([\\w(.|,)]+) .*(mobile ?safari|safari)",
  "Other",
  "isHuawei",
  "isOpera",
  "isFirefox",
  "isEdge",
  "isIE",
  "isChrome",
  "isSafari",
  "isOther",
  "Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.",
  "((file|https?):\\/\\/localhost)",
  "https?:\\/\\/([0-9]{1,3}(\\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})",
  "(Module._compile|Object.Module|Module.load|Function.Module._load)",
  "Fail to construct 'URL': Invalid URL",
  "stack",
  "\n",
  "estimate",
  "quota",
  "2300000000",
  "serviceWorker",
  "bdmsCheck",
  "success",
  "deleteDatabase",
  "error",
  "getDirectory",
  "out of memory",
  "HTMLAllCollection",
  "h",
  "w",
  "cefSharp",
  "CefSharp",
  "eoapi",
  "eoWebBrowserDispatcher",
  "isExtended",
  "onchange",
  "frames",
  "history",
  "[object Navigator]",
  "[object HTMLDocument]",
  "[object Document]",
  "[object Location]",
  "[object History]",
  "[native code]",
  "PluginArray",
  "MSPluginsCollection",
  "isWindows",
  "isLinux",
  "isHarmonyOS",
  "isAndroid",
  "isApple",
  "isMacOS",
  "isiOS",
  "^((file|https?):\\/\\/localhost)",
  "^https?:\\/\\/([0-9]{1,3}(\\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})",
  "__non_webpack_require__",
  "global",
  "process",
  "[object process]",
  "title",
  "node",
  "_phantom",
  "callPhantom",
  "__nightmare",
  "Audio",
  "CanvasRenderingContext2D",
  "__webdriver_evaluate",
  "__selenium_evaluate",
  "__webdriver_script_function",
  "__webdriver_script_func",
  "__webdriver_script_fn",
  "__fxdriver_evaluate",
  "__driver_unwrapped",
  "__webdriver_unwrapped",
  "__driver_evaluate",
  "__selenium_unwrapped",
  "__fxdriver_unwrapped",
  "_Selenium_IDE_Recorder",
  "_selenium",
  "calledSelenium",
  "_WEBDRIVER_ELEM_CACHE",
  "ChromeDriverw",
  "driver-evaluate",
  "webdriver-evaluate",
  "selenium-evaluate",
  "webdriverCommand",
  "webdriver-evaluate-response",
  "__webdriverFunc",
  "__$webdriverAsyncExecutor",
  "__lastWatirAlert",
  "__lastWatirConfirm",
  "__lastWatirPrompt",
  "$chrome_asyncScriptInfo",
  "$cdc_asdjflasutopfhvcZLmcfl_",
  "callSelenium",
  "__phantomas",
  "HeadlessChrome",
  "connection",
  "rtt",
  "userAgentData",
  "brands",
  "HarmonyOS",
  "droid ([\\w.]+)\\b.+(harmonyos)",
  "OpenHarmony",
  "Android",
  "droid ([\\w.]+)\\b.+(android[- ]x86)",
  "(android)[-/ ]?([\\w.]*)",
  "iOS",
  "ip[honead]{2,4}\\b(?:.*os ([\\w]+) like mac|; opera)",
  "(?:\\/|\\()(ip(?:hone|od)[\\w, ]*)(?:\\/|;)",
  "\\((ipad);[-\\w),; ]+apple",
  "applecoremedia\\/[\\w.]+ \\((ipad)",
  "\\b(ipad)\\d\\d?,\\d\\d?[;\\]].+ios",
  "\\b(crios)\\/([\\w.]+)",
  "MacOS",
  "(mac os x) ?([\\w. ]*)",
  "(macintosh|mac_powerpc\\b)(?!.+haiku)",
  "Windows",
  "microsoft (windows) (vista|xp)",
  "(windows) nt 6\\.2; (arm)",
  "(windows)[/ ]?([ntce\\d. ]+\\w)(?!.+xbox)",
  "(windows (?:phone(?: os)?|mobile))[/ ]?([\\d.\\w ]*)",
  "(win(?=3|9|n)|win 9x )([nt\\d.]+)",
  "Linux",
  "(linux) ?([\\w.]*)",
  "android",
  "Apple",
  "mac|iphone|ipad|ipod",
  "linux",
  "win",
];

// 用于把 base64 解码后的字符转换成 真实字节 (0–255)
// Uint8Array.from(arrayLike, mapFn, thisArg)
// 对应到你的代码：
// 参数	含义
// r.slice(8)	输入：去掉前 8 字节的字符串
// _	对每个字符进行解密/转换的函数
// e % 256	thisArg，供 _ 内部当 key 或参数用
function _(t, r) {
  return (t.charCodeAt(0) ^ (this + (this % 10) * r) % 256) >>> 0;
}

// for (i = 0; i < data.length; i++) {
//   data[i] = (data.charCodeAt(0) ^ (key + (key % 10) * i) % 256) >>> 0;
// }

// LEB128 解码器
// 功能：从字节流 r.d 中按 LEB128 格式读取一个带符号的整数
// 支持：
// 任意长度整数
// 多字节编码（7bit chunks）
// 检测负数并扩展符号位
// 等价于：
// SLEB128_decode()
function W(r) {
  // t = 最终结果
  // e = 当前解析的字节索引 (shift 次数)
  var t = 0;
  var e = 0;

  while (true) {
    // 通过 K() 获取当前读取位置，然后从 r 中取一个字节
    var n = r.d[r.i++];

    // 取低 7 位，并按位置 e 左移
    var value = (n & 0x7f) << (7 * e);

    // 累加到结果
    t |= value;

    e++;

    // 如果最高位未置位，就表示值结束
    if ((n & 0x80) === 0) {
      break;
    }
  }

  return t;
}

// UTF-8 解码器
// 在从字节流中解码 UTF-8 字符串（UTF-8 解码器）。
// 它是一个 手写的 UTF-8 解码函数，用来把字节数组转换成 Unicode 字符串。
function K(t) {
  // r = 当前正在构造的 codepoint（UTF-8 解码状态）
  var r = -1;

  // e = 最终输出的 codepoint 数组
  var e = [];

  while (true) {
    // 从 t.d 中读取一个字节，并前进游标 t.i
    var n = t.d[t.i++];

    // 1) 当 n 是 10xxxxxx (UTF-8 continuation byte)
    if (n >= 128 && n < 192) {
      // 取低 6 bit，加到 r 的末尾
      r = (r << 6) + (n & 0x3f);
    } else {
      // 如果 r >= 0，表示上一个 codepoint 结束，推入数组
      if (r >= 0) {
        e.push(r);
      }

      // 2) 当 n < 128 → 0xxxxxxx → 单字节字符
      if (n < 128) {
        r = n;
      }

      // 3) 当 n < 224 → 110xxxxx → 2字节序列的首字节
      else if (n < 224) {
        r = n & 0x1f;
      }

      // 4) 当 n < 240 → 1110xxxx → 3字节序列的首字节
      else if (n < 240) {
        r = n & 0x0f;
      }

      // 5) 当 n < 248 → 11110xxx → 4字节序列的首字节
      else {
        if (!(n < 248)) {
          break; // 超过合法 UTF-8 范围，跳出
        }
        r = n & 0x07;
      }
    }
  }

  // 将 codepoint 数组转换成字符串
  return String.fromCodePoint.apply(null, e);
}

var U,
  I,
  F,
  M,
  B,
  Q,
  H,
  q,
  N,
  G,
  Z = [], // 字符串常量池
  z = [], // 函数/指令表（每个元素描述一个函数或 opcode 信息）
  V = new Map(), // 映射（可能是 cache 或 runtime environment）
  Y = new Map(); // 映射（可能是 cache 或 runtime environment）

// vm入口
// t: 指令序列索引，z[t]
// r: 目前均传void 0，没发现它的意义
// e: 参数
// n：执行当前指令依赖的对象
function J(t, r, e, n) {
  // 如果 Z 还没有被初始化，就先初始化（只执行一次）
  if (!Z.length) {
    (function (t) {
      // --- Step 1: 先从 Base64 解码，并做一次自定义 XOR 处理 ---
      var r = (function (t) {
        var raw = atob(t);

        var keysum = 0;
        for (var i = 4; i < 8; ++i) keysum += raw.charCodeAt(i);

        return {
          d: pako.inflateRaw(
            // C = 自定义 deflate 解压器
            Uint8Array.from(
              raw.slice(8), // 从第 8 字节开始才是压缩数据
              _, // map 函数：自定义 XOR 解密
              keysum % 256 // thisArg：参与 XOR
            )
          ),
          i: 0,
        };
      })(t);

      // --- Step 2: 清空原来缓存 ---
      Z.length = 0;
      z.length = 0;
      V.clear();
    //
    // 解密后的结构格式
    // +-----------------+
    // | 常量池 1001      |
    // +-----------------+
    // | 指令元数据       |
    // +-----------------+
    // | 指令序列         |
    // +-----------------+

      // --- Step 3: 读取 Z 表 ---
      var countStringPool = W(r);
      for (var n = 0; n < countStringPool; ++n) {
        Z.push(K(r)); // K = UTF-8 解码 , 长度1001
      }

      // --- Step 4: 读取 z（核心表） ---
      var countCode = W(r);
      for (n = 0; n < countCode; ++n) {
        var i = W(r); // 参数个数
        var flag = Boolean(W(r)); //

        // --- 4.1 读取 s（二维数组，每项4整数） ---
        var s = [];
        var c = W(r);
        for (var a = 0; a < c; ++a) {
          s.push([W(r), W(r), W(r), W(r)]);
        }

        // --- 4.2 读取 codes（整数数组） ---
        var codes = [];
        var l = W(r);
        for (var p = 0; p < l; ++p) codes.push(W(r));

        // --- 4.3 写入表 ---
        z.push([codes, i, flag, s]); // 四元组结构
      }
    })(
      "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"
    ); // 这里的 "xxxxx" 是 Base64 编码的原始数据
  }

  for (i = 0; i < z.length; i++) {
    if (z[i][0].length == 288) console.log("code.length[288] = " + i);
    if (z[i][0].length == 484) console.log("code.length[484] = " + i);
  }

  // --- Step 5: 实际执行（VM 调用或解释执行） ---
  X(z[t], r, e, n);

}

// 将字节码函数包装成可调用的 JavaScript 函数
// 指令-> vm函数缓存
// t函数索引，r上下文对象
function D(t, r) {
  // 从 z 数组中取出原始指令
  var e = z[t];

  // 如果 Y 中记录了 t 对应的包装函数，则从 V 中删掉旧的包装记录
  if (Y.has(t)) {
    var oldWrapper = Y.get(t);
    V.delete(oldWrapper);
  }

  // 构造一个新的包装函数 n
  var n = function () {
    // 调用 X，对 e 进行包装调用
    // this 保持原样，arguments 原样透传
    return X(e, this, arguments, r);
  };

  // 记录新生成的包装函数
  Y.set(t, n); // Y: t → n ，指令表索引 -> "对应函数入口" 缓存
  V.set(n, [e, r]); // V: n → [原始指令, 依赖对象]， 指令表 -> [指令， 依赖对象] 缓存

  return n; // 返回新的包装函数
}

// 解释器入口
// t: 指令序列索引，z[t]
// r: 代理obj_context，不传时为空对象{}， 但大多不用，因为n大多都传
// e: 参数
// n：执行当前指令依赖的对象
function X(t, r, e, n) {
  // p是当前栈、寄存器指针pc,
  // l是返回值，该返回值可以是一个异常
  // f是函数执行的状态码
  // o是指令链序列
  // i是boolean值， 是否动态生成代理this_context
  // u退出码
  // s是虚拟机执行当前指令的栈，初始化为 [调用上下文, 局部变量v, 传入参数], 切换时保存的是U
  // U是运行时的栈(本地栈)，可以理解为函数栈帧，U前几位占位为context(如依赖，和传入参数、参数展开等)、 临时备用栈和v之间数据切换，是s的拷贝，或者就是指向同一块内存
  // 有时候它也作为依赖的context，ObjectContext使用，这里包含了此指令调用所需要的所有变量和函数

  // c 代理this_context，传入空时为空对象{}
  // a是vm指令pc
  // h是保存状态的栈，用于上下文切换
  // v是操作数栈，当前函数执行时的本地栈, v[0]当前要被执行或者修改的，v[1 - 2] 参数 ，或者理解为寄存器
  // Z[]是一个函数数组，通过索引来调用指定函数
  //////////
  // U, s, v特定变量内部指向的内存是相同的，它会导致U, s, v中的数据发生变化，其他的栈也会跟着一起同步变化
  // 每次进入g函数或者vm函数时，v U s都会被更新，但是之前的状态会被保存

  // h相当于保存状态的栈，进入函数前时push，退出函数时pop，仅保存当前函数相关的caller的一个栈帧，退出函数h消失
  // h pop的数据会被保存在一个临时g数组中，通过g数组取保存的状态值，退出函数g消失
  var o,
    i,
    u,
    s,
    c,
    a,
    f,
    l,
    p = -1,
    v = [],
    h = [];
  g(t, r, e, n); // 初始化上下文
  do {
    try {
      d(); // 虚拟机解释器
    } catch (t) {
      f = 3;
      l = t;
    }
  } while (y()); // 处理返回值
  return l;

  // function J定义如下(t, void 0, arguments, {get 0 , set 0 函数 ...})
  // t = z[731] // z[n] = [指令序列， 参数个数， 是否需要生成代理obj_context，退出码、异常 [...]]
  // r = undefined， void 0 ; this 或者代理obj_context
  // e = arguments
  // n = obj_context, this_object,相当于t执行需要的context，它内部包含t执行需要的变量和方法
  // 初始化上下文
  function g(t, r, e, n) {
    // t[1]也代表参数个数，这里取最小
    var p = Math.min(e.length, t[1]);

    // v 相当于函数运行时的局部变量对象（模拟寄存器）
    var v = {};

    // 定义 v.length，但不让其被枚举
    Object.defineProperty(v, "length", {
      value: e.length,
      writable: true,
      enumerable: false,
      configurable: true,
    });

    // 解构参数 t，对应 t = [o, 参数数量, i, u]
    o = t[0]; // 指令序列
    i = t[2]; // 是否动态生成栈
    u = t[3]; // 退出码

    // s 是虚拟机执行当前指令的栈，初始化为 [调用上下文, 局部变量v, 传入参数]
    s = [n, v]; // 栈底是obj_context, 本地变量栈， 调用该函数传递的参数

    // 把传入参数压入s
    for (var h = 0; h < p; ++h) s.push(e[h]);

    if (i) {
      // i 为真：把传入参数复制到本地栈
      c = r; // 代理obj_context, 引用处 33 === t ? void 0 : c
      for (h = 0; h < e.length; ++h) v[h] = e[h];
    } else {
      // 类似动态生成代理obj_context
      // i 为假：使用 getter/setter 代理 s 中参数
      c = r == null ? globalThis : Object(r);

      // 生成访问器函数
      var g = function (t) {
        if (t < p) {
          // 对前 p 项设置 getter/setter 绑定到 s[t+2]
          Object.defineProperty(v, t, {
            get: function () {
              return s[t + 2];
            },
            set: function (r) {
              s[t + 2] = r;
            },
            enumerable: true,
            configurable: true,
          });
        } else {
          // 超出部分直接赋值
          v[t] = e[t];
        }
      };

      // 为每个参数设置代理或直接赋值
      for (h = 0; h < e.length; ++h) g(h);
    }

    // 初始化执行状态
    a = 0;
    f = 0;
    l = void 0;
  }

  function d() { // 虚拟机解释器
    // 源d函数
    for (;;) {
      var t = o[a++];
      switch (t) {
        case 0: {
          // 函数调用：取函数指针并执行（vm函数或原生函数）
          var r = o[a++];
          p -= r;
          var e = v.slice(p + 1, p + r + 1),
            n = v[p--],
            d = v[p--];
          if (typeof n !== "function") {
            f = 3;
            l = new TypeError(typeof n + " is not a function");
            return;
          }
          var y = V.get(n);
          if (y) {
            // 保存上下文
            h.push([o, i, u, s, c, a, f, l]);
            // 进入vm
            g(y[0], d, e, y[1]);
          } else {
            var m = n.apply(d, e);
            v[++p] = m;
          }

          break;
        }

        case 1: // <= 比较
          var w = v[p--];
          v[p] = v[p] <= w;
          break;

        case 2: // > 比较
          w = v[p--];
          v[p] = v[p] > w;
          break;

        case 3: {
          // 枚举对象属性名并保存为局部变量
          var x = o[a++],
            S = v[p--],
            P = [];
          for (var j in S) P.push(j);
          s[x] = [P, S];
          break;
        }

        case 4: // 从对象枚举属性名中取下一个有效 key
          x = o[a++];
          var O = v[p--],
            R = v[p--];
          P = s[x];
          j = void 0;
          do {
            j = P[0].shift();
          } while (j !== void 0 && !(j in P[1]));
          if (j !== void 0) {
            R[O] = j;
            v[++p] = true;
          } else {
            v[++p] = false;
          }
          break;

        case 5: // 获取变量名并调用 b()
          x = o[a++];
          var A = Z[x] /* 源Z[x]*/,
            E = b(A, i);
          v[++p] = E;
          v[++p] = A;
          break;

        case 6: // !== 比较
          w = v[p--];
          v[p] = v[p] !== w;
          break;

        case 7: // 创建新对象 {}
          v[++p] = {};
          break;

        case 8: // 对象访问 obj[key]
          var k = v[p--];
          v[p] = v[p][k];
          break;

        case 9: // 入栈 true
          v[++p] = true;
          break;

        case 10: // 入栈 undefined
          v[p] = void 0;
          break;

        case 11: // 模运算 a % b
          E = v[p--];
          v[p] %= E;
          break;

        case 12: // 按位与 &
          w = v[p--];
          v[p] = v[p] & w;
          break;

        case 13: // instanceof 判断
          S = v[p--];
          v[p] = v[p] instanceof S;
          break;

        case 14: // 数组/对象元素赋值 obj[key] = val
          E = v[p--];
          var T = v[p--];
          S = v[p--];
          S[T] = E;
          break;

        case 15: // 给 globalThis 赋值
          x = o[a++];
          E = v[p--];
          var C = Z_ARRAY[x]; /* 源Z[x]*/
          if (i && !(C in globalThis)) {
            f = 3;
            l = new ReferenceError(C + " is not defined");
            return;
          }
          globalThis[C] = E;
          break;

        case 16: // 对象成员赋值 obj[key] = value（key 从栈中读取）
          var L = v[p--];
          S = v[p--];
          S[L] = v[p];
          break;

        case 17: // if 条件跳转（为假则跳转 offset）
          var U = o[a++];
          if (!v[p]) {
            a += U;
          } else {
            --p;
          }
          break;

        case 18: // 复制栈顶值一份（用于保留）
          E = v[p];
          v[++p] = E;
          break;

        case 19: // 无符号右移 >>>
          w = v[p--];
          v[p] = v[p] >>> w;
          break;

        case 20: // obj[prop] = val（属性名从Z表中取）
          x = o[a++];
          E = v[p--];
          S = v[p--];
          S[Z_ARRAY[x] /* 源Z[x]*/] = E;
          break;

        case 21: // 减法 -=
          E = v[p--];
          v[p] -= E;
          break;

        case 22: // 跳过（noop / 空操作）
          if (f !== 0) return;
          break;

        case 23: // 条件等于跳转（v[p] === val → 跳转 offset）
          U = o[a++];
          E = v[p--];
          if (v[p] === E) {
            --p;
            a += U;
          }
          break;

        case 24: // typeof 判断
          v[p] = typeof v[p];
          break;

        case 25: // delete obj[prop]
          var I = v[p--];
          S = v[p--];
          E = delete S[I];
          v[++p] = E;
          break;

        case 26: // 弹出栈顶（--p）
          --p;
          break;

        case 27: // 入栈 false
          v[++p] = false;
          break;

        case 28: // 入栈 NaN
          v[++p] = NaN;
          break;

        case 29: // 布尔取反 !
          v[p] = !v[p];
          break;

        case 30: // obj[Z_ARRAY[x] /* 源Z[x]*/] 属性读取
          x = o[a++];
          v[p] = v[p][Z_ARRAY[x] /* 源Z[x]*/];
          break;

        case 31: // 条件跳转（为真则跳 offset）
          U = o[a++];
          if (v[p]) {
            a += U;
          } else {
            --p;
          }
          break;

        case 32: // < 比较
          w = v[p--];
          v[p] = v[p] < w;
          break;

        case 33: // 入栈 undefined
          v[++p] = void 0;
          break;

        case 34: // this, 入栈动态代理对象，没有则为空对象{}
          v[++p] = c;
          break;

        case 35: // 有符号右移 >>
          w = v[p--];
          v[p] = v[p] >> w;
          break;

        case 36: // 转为数字（单目 +）
          v[p] = +v[p];
          break;

        case 37: // 按位非 ~
          v[p] = ~v[p];
          break;
        case 38: // 取下一条指令数据直接入栈
          v[++p] = o[a++];
          break;

        case 39: {
          // 多值截取入栈（将 v 栈中 p - F + 1 到 p + 1 的 F 个值整体作为数组入栈）
          var F = o[a++];
          v[(p = p - F + 1)] = v.slice(p, p + F);
          break;
        }

        case 40: {
          // 前缀递增 obj[prop]，返回递增后值
          var M = v[p--];
          S = v[p--];
          E = ++S[M];
          v[++p] = E;
          break;
        }

        case 41: // 条件跳转（如果为假跳 offset）
          U = o[a++];
          if (!v[p--]) a += U;
          break;

        case 42: // 除法 a / b
          E = v[p--];
          v[p] /= E;
          break;

        case 43: // 单目负号（取负）
          v[p] = -v[p];
          break;

        case 44: {
          // 前缀递减 obj[prop]，返回递减后值
          var B = v[p--];
          S = v[p--];
          E = --S[B];
          v[++p] = E;
          break;
        }

        case 45: // 乘法 *
          E = v[p--];
          v[p] *= E;
          break;

        case 46: // 入栈 +Z_ARRAY[x] /* 源Z[x]*/（Z 是变量名表）
          x = o[a++];
          v[++p] = +Z_ARRAY[x] /* 源Z[x]*/;
          break;

        case 47: {
          // 定义 getter 属性
          x = o[a++];
          var Q = v[p--];
          Object.defineProperty(v[p], Z_ARRAY[x] /* 源Z[x]*/, {
            get: Q,
            enumerable: true,
            configurable: true,
          });
          break;
        }

        case 48: {
          // 定义 setter 属性
          x = o[a++];
          var H = v[p--];
          Object.defineProperty(v[p], Z_ARRAY[x] /* 源Z[x]*/, {
            set: H,
            enumerable: true,
            configurable: true,
          });
          break;
        }

        case 49: // 抛出异常 throw
          f = 3;
          l = v[p--];
          return;

        case 50: {
          // 后缀递增 obj[prop]，返回 ++ 前的值
          var q = v[p--];
          S = v[p--];
          E = S[q]++;
          v[++p] = E;
          break;
        }

        case 51: // 按位或 |
          w = v[p--];
          v[p] = v[p] | w;
          break;

        case 52: // try 跳转（保存跳转位置）
          U = o[a++];
          f = 1;
          l = a + U;
          return;

        case 53: // 无条件跳转
          U = o[a++];
          a += U;
          break;

        case 54: {
          // 结构赋值返回（递归访问 s 结构并赋值）
          var N = o[a++];
          x = o[a++];
          U = s;
          while (N-- > 0) {
            U = U[0];
          }
          U[x] = v[p--];
          break;
        }

        case 55: // in 操作符：v[p] in v[p--]
          S = v[p--];
          v[p] = v[p] in S;
          break;

        case 56: // 左移 <<
          w = v[p--];
          v[p] = v[p] << w;
          break;

        case 57: // v[p] === v[p - 1]
          w = v[p--];
          v[p] = v[p] === w;
          break;

        case 58: // v[p] == v[p - 1]
          w = v[p--];
          v[p] = v[p] == w;
          break;

        case 59: // 取函数原型，调用构造函数，生成对象入栈
          r = o[a++];
          for (var G = [void 0]; r > 0; ) G[r--] = v[p--];
          var z = v[p--];
          m = new (Function.bind.apply(z, G))();
          v[++p] = m;
          break;

        case 60: // 取函数，如 Array 等，从 globalThis 中找
          x = o[a++];
          var Y = Z_ARRAY[x]; /* 源Z[x]*/
          if (!(Y in globalThis)) {
            f = 3;
            l = new ReferenceError(Y + " is not defined");
            return;
          }
          E = globalThis[Y];
          v[++p] = E;
          break;

        case 61: // 取数组参数项入栈
          N = o[a++];
          x = o[a++];
          U = s;
          while (N > 0) {
            U = U[0];
            --N;
          }
          v[++p] = U;
          v[++p] = x;
          break;

        case 62: // v[p] != v[p - 1]
          w = v[p--];
          v[p] = v[p] != w;
          break;

        case 63: // D(o[a++], s)，调用虚拟函数
          E = D(o[a++], s);
          v[++p] = E;
          break;

        case 64: // v[p] >= v[p - 1]
          w = v[p--];
          v[p] = v[p] >= w;
          break;

        case 65: // 向栈压入 Infinity
          v[++p] = 1 / 0;
          break;

        case 66: // 属性自减：S[J]--
          var J = v[p--];
          E = (S = v[p--])[J]--;
          v[++p] = E;
          break;
        case 67: // 设置对象属性：Object.defineProperty(v[p], Z_ARRAY[x] /* 源Z[x]*/, { value: v[p+1], ... })
          x = o[a++];
          E = v[p--];
          Object.defineProperty(v[p], Z_ARRAY[x] /* 源Z[x]*/, {
            value: E,
            writable: !0,
            configurable: !0,
            enumerable: !0,
          });
          break;

        case 68: // 栈顶两个数相加：v[p] += v[p+1]
          E = v[p--];
          v[p] += E;
          break;

        case 69: // 获取globalThis[Z_ARRAY[x] /* 源Z[x]*/]的typeof类型，并入栈
          x = o[a++];
          var X = Z[x]; /* 源Z[x]*/
          E = typeof globalThis[X];
          v[++p] = E;
          break;

        case 70: // 栈顶两个值按位异或
          w = v[p--];
          v[p] = v[p] ^ w;
          break;

        case 71: // 条件跳转：若v[p--]为真，则a += U
          U = o[a++];
          if (v[p--]) a += U;
          break;

        case 72: // 若globalThis[Z_ARRAY[x] /* 源Z[x]*/]不存在，则设置为undefined
          x = o[a++];
          var W = Z[x]; /* 源Z[x]*/
          if (!(W in globalThis)) globalThis[W] = void 0;
          break;

        case 73: // 将Z_ARRAY[x] /* 源Z[x]*/常量入栈
          x = o[a++];
          v[++p] = Z_ARRAY[x] /* 源Z[x]*/;
          break;

        case 74: // 深度获取依赖参数，并入栈
          N = o[a++];
          x = o[a++];
          U = s;
          while (N > 0) {
            U = U[0];
            --N;
          }
          E = U[x];
          v[++p] = E;
          break;

        case 75: // 入栈null
          v[++p] = null;
          break;

        default: // 非法指令，设置返回值并结束执行
          f = 2;
          l = v[p--];
          break;
      }
    }
  }

  function y() {
    // 函数退出控制流
    var t = a,
      r = u;

    switch (f) {
      case 1: {
        for (var e = r.length - 1; e >= 0; --e) {
          var n = r[e];
          if (n[0] < t && t <= n[3]) {
            if (t <= n[2] && n[2] !== n[3]) {
              a = n[2];
            } else {
              a = l;
              f = 0;
              l = void 0;
            }
            return true;
          }
        }
        throw new SyntaxError("Illegal statement");
      }

      case 2: {
        for (var e = r.length - 1; e >= 0; --e) {
          var n = r[e];
          if (n[0] < t && t <= n[2] && n[2] !== n[3]) {
            a = n[2];
            return true;
          }
        }

        var g = h.pop();
        if (!!g) {
          v[++p] = l;
          o = g[0];
          i = g[1];
          u = g[2];
          s = g[3];
          c = g[4];
          a = g[5];
          f = g[6];
          l = g[7];
          return true;
        }

        return false;
      }

      case 3: {
        for (var e = r.length - 1; e >= 0; --e) {
          var n = r[e];
          if (n[0] < t) {
            if (t <= n[1] && n[1] !== n[2]) {
              a = n[1];
              v[++p] = l;
              f = 0;
              l = void 0;
              return true;
            }
            if (t <= n[2] && n[2] !== n[3]) {
              a = n[2];
              return true;
            }
          }
        }

        var g = h.pop();
        if (g) {
          o = g[0];
          i = g[1];
          u = g[2];
          s = g[3];
          c = g[4];
          a = g[5];
          return y(); // 递归调用自己继续处理异常
        }
        throw l;
      }
      default:
        break;
    }
    return true;
  }

  function b(t, r) {
    var e = Object.create(null);
    return (
      Object.defineProperty(e, t, {
        get: function () {
          if (t in globalThis) return globalThis[t];
          throw new ReferenceError(t + " is not defined");
        },
        set: function (e) {
          if (r && !(t in globalThis))
            throw new ReferenceError(t + " is not defined");
          globalThis[t] = e;
        },
      }),
      e
    );
  }
}

/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

// 环境检测部分代码
// 这个函数可以得到匹配的regs，并且能返回对应的名字
function fg_regs() {}

function beforeMkAb() {
  // 先测试是否包含a_bogus，不存在依次执行下列动作
  // 对应vm函数J0_get14_pr(),无参数，指令长度为12
  t1 = +Date.now();
  // 得到uri， 通过对URL.searchParams.toString()
  // 类似"device_platform=webapp&aid=6383&channel=channel_pc_web&pc_client_type=1&pc_libra_divert=Windows&update_version_code=170400&support_h265=1&support_dash=1&version_code=170400&version_name=17.4.0&cookie_enabled=true&screen_width=2560&screen_height=1440&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=138.0.0.0&browser_online=true&engine_name=Blink&engine_version=138.0.0.0&os_name=Windows&os_version=10&cpu_core_num=20&device_memory=8&platform=PC&downlink=4&effective_type=4g&round_trip_time=50&webid=7482403095261316617&uifid=c4a29131752d59acb78af076c3dbdd52744118e38e80b4b96439ef1e20799db0e06c5be9532d94f0b7725e552b444440ba2c1788bfe5c8a70c5c57ab949d98c92463baa114bc91665f6b6f521e7d9a95ab08e303534db5095aeb069825e1f5e2bf4a4d0f4046a281efabce92529f452a210f0f0177b5e702cbf977dbc27d98980ac8f88aee8c3a95a21b96af00089db82838edad8a54dca3f57bbbc31132d6bed21846d5a9268a6f4485626786e98bd17c3347439baef5c777c1c3db74544de5&msToken=yVSQmgAPOmNENpmIx4HBNzrkB3e9HnHE6AqvZk47tvUgSG4tJyurEnHmkQ81hs8tYpe_3STFrVkx-Lz9rEkhsWFsnk0VgxrxRmJ5AvCM0zkCnfMKCgkeSxHMmsAYoD7jw46lcr-UYbRkR_1KySxYNbH1QEyaPWYKVo5tu-FAO3Gn0GDDIljjzoM%3D"
  uri = URLSearchParams.toString(); // 赋值给临时的U6
  // 先测试是否包含a_bogus，不包含则先做环境监测，太多了，不分析了，和功能无关
  // 类似这样的数据10206569.299999952
  var env_test_start = performance.now(); // 临时U5
  // 对应vm函数J0_get12_lr,code长度21
  // 取navigator
  navigator_proto = navigator.__proto__;
  ink_obj = {};
  var dnow = Date.now(); // 1753084107074
  dsub = dnow - 1;
  // 定义ink即：{"ink": 1753084107073 }
  ink = Object.defineProperty(ink_obj, "ink", {
    value: dsub,
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  navigator_proto.vendorSubs = ink;

  vp = typeof null; // 'object'
  vp = vp !== "string"; // true 没看到具体哪里用了，指令向后跳转
  ustr = ""; // 临时U7

  // "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
  userAgent = navigator.userAgent; // 临时U8
  // 取userAgent测试是否包含字符baiduboxapp,AlipayClient
  found = userAgent.indexOf("baiduboxapp"); // -1
  if (found >= 0) {
    // 我们这里没有匹配， 我们忽略它做什么
    // 这里继续向后跳转
  }

  found = userAgent.indexOf("AlipayClient");
  if (found >= 0) {
    // 同上
  }

  J0_get18_vr = {
    aid: 6383,
    pageId: 11881,
    boe: false,
    ddrt: 8.5,
    paths: {
      include: [
        { source: "^\\/webcast\\/" },
        { source: "^\\/aweme\\/v1\\/" },
        { source: "^\\/aweme\\/v2\\/" },
        { source: "\\/v1\\/message\\/send" },
        { source: "^\\/live\\/" },
        { source: "^\\/captcha\\/" },
        { source: "^\\/ecom\\/" },
      ],
      exclude: [{ source: "/monitor_browser/collect/batch" }],
    },
    track: {
      mode: 0,
      delay: 300,
      paths: [],
    },
    slU: "",
    dump: true,
    rpU: "",
    ic: 8.5,
  };

  pageId = J0_get18_vr["pageId"];
  aid = J0_get18_vr["aid"];
  sdkVersion = "1.0.1.19-fix.01";

  // J0_get13_cr, code长度为1834, 这个是生产ab的vm函数
  // 进入函数J0_get13_cr(1, 0, 8, uri, "", userAgent, pageId=11881, aid=6383, sdkVersion)

  // 这里就正式进入ab生成vm函数了
  // U4++
  U12 = 11; // 到此所有传入参数都被拷贝到U数组占位位置
  // 后面略，进入还原函数看
}

// 可用可不用
// 此方法不是性能优化，而是a_bogus执行过程中的逻辑
// 主要检测浏览器相关，测试是否使用了浏览器作弊
// 这些逻辑包含对环境等检测，通过检测的值组合生成Ux，例如U16
// makeABogus是把最终结果直接赋值给了U数组
function envDetectU16() {
  Date.now(); // 1753098511514

  // 以下是生成U16的逻辑
  // 满足下面条件才执行call方法
  if (typeof window !== "undefined");
  if (typeof navigator !== "undefined");
  if (typeof document !== "undefined");
  if (typeof location !== "undefined");
  if (typeof history !== "undefined");
  Object.prototype.toString.call(navigator) === "[object Navigator]"; // true
  Object.prototype.toString.call(document) === "[object HTMLDocument]"; // true
  Object.prototype.toString.call(location) === "[object Location]"; // true
  Object.prototype.toString.call(history) === "[object History]"; // true
  // 和这些方法
  doc_canvas = document.createElement("canvas");
  // 28个字节指令完成
  // [74,0,2,24,73,0,6,31,18,74,0,2,18,30,9,0,0,18,30,205,73,914,0,1,38,0,1,76]
  typeof doc_canvas.toDataURL() !== "function"; // false
  doc_canvas_str = doc_canvas.toDataURL().toString(); // "function toDataURL() { [native code] }"
  doc_canvas = doc_canvas_str.indexOf("[native code]"); // 23
  doc_canvas_index <= 0; // false

  // 重复
  typeof navigator.toString() !== "function"; // false
  nav_str = navigator.toString().toString(); // "function toString() { [native code] }"
  nav_index = nav_str.indexOf("[native code]"); // 22
  nav_index <= 0; // false

  if (typeof PluginArray !== "undefined") {
    plugins = navigator.plugins;
    if (plugins instanceof PluginArray);
  }

  mask = typeof MSPluginsCollection !== "undefined"; // 没有定义，就没有进一步检测
  mask = (+mask << 1) | 1;

  // 以下去非的操作均是测试是否存在该对象或者api
  !window.screen;
  !window.eval;
  !!navigator.connection; // NetworkInformation
  navigator.connection.rtt === 0; // 100, false

  // navigator.userAgentData.brands检测
  !navigator.userAgentData; // NavigatorUAData
  // navigator.userAgentData.brands得到如下数据：
  // [
  //         {
  //                 "brand": "Not)A;Brand",
  //                 "version": "8"
  //         },
  //         {
  //                 "brand": "Chromium",
  //                 "version": "138"
  //         },
  //         {
  //                 "brand": "Google Chrome",
  //                 "version": "138"
  //         }
  // ]
  navi_brands = navigator.userAgentData.brands;
  navi_brands === null;
  navi_brands === void 0;
  navi_brands.length === 0;

  // 屏幕测试
  window.innerWidth === 800; // 后续看了这个是否为true
  window.innerHeight === 600;
  window.outerWidth === 0; // 后续看了这个是否为true
  window.outerHeight === 0;

  // 5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36
  !!navigator.appVersion; // 是否支持获取navigator.appVersion
  // HeadlessChrome检测
  // 这三项也形成了一个数组[false, false, false]
  navigator.appVersion.indexOf("HeadlessChrome") >= 0; // -1
  typeof navigator.userAgent !== "string";
  navigator.userAgent.indexOf("HeadlessChrome") >= 0;

  // 这里应该存在一个 << 1, 应该是这样，落掉了
  // 应该没拉下，因为上面补充了mask = (+mask << 1) | 1;
  //mask |= +(navigator.userAgent.indexOf("HeadlessChrome") >= 0) << 1;

  // webdriver相关检测
  is_hit_nav_webdriver = !navigator.webdriver === true; // false
  Object.getOwnPropertyDescriptor(navigator, "webdriver") !== void 0; // undefined

  // 对浏览器模拟器检测
  win_emulators = [
    "_selenium",
    "callSelenium",
    "_Selenium_IDE_Recorder",
    "_phantom",
    "__phantomas",
    "__nightmare",
    "callPhantom",
  ];

  is_hit_win_emu = win_emulators.some((e) => {
    if (window[e] || Object.getOwnPropertyDescriptor(window, e) !== undefined) {
      return true;
    }
  });

  is_hit_win_emu = false; // is_hit_win_emu必须为false
  // 继续下面检测

  doc_emu_cmds = [
    "__webdriver_evaluate",
    "__selenium_evaluate",
    "__webdriver_script_function",
    "__webdriver_script_func",
    "__webdriver_script_fn",
    "__fxdriver_evaluate",
    "__driver_unwrapped",
    "__webdriver_unwrapped",
    "__driver_evaluate",
    "__selenium_unwrapped",
    "__fxdriver_unwrapped",
    "_Selenium_IDE_Recorder",
    "_selenium",
    "calledSelenium",
    "_WEBDRIVER_ELEM_CACHE",
    "ChromeDriverw",
    "driver-evaluate",
    "webdriver-evaluate",
    "selenium-evaluate",
    "webdriverCommand",
    "webdriver-evaluate-response",
    "__webdriverFunc",
    "__webdriver_script_fn",
    "__$webdriverAsyncExecutor",
    "__lastWatirAlert",
    "__lastWatirConfirm",
    "__lastWatirPrompt",
    "$chrome_asyncScriptInfo",
    "$cdc_asdjflasutopfhvcZLmcfl_",
  ];

  is_hit_doc_emu = doc_emu_cmds.some((e) => {
    if (
      document[e] ||
      Object.getOwnPropertyDescriptor(document, e) !== undefined
    ) {
      return true;
    }
  });

  is_hit_doc_emu = false; // 同样is_hit_doc_emu必须为false

  // 必须均为false
  webdriver_detect = [is_hit_nav_webdriver, is_hit_win_emu, is_hit_doc_emu];
  webdriver_detect = webdriver_detect.filter((val) => {
    return val === true; // 没执行任何
  });

  mask |= +(webdriver_detect.length >= 3) << 2; // 结果还是为1

  // 和上面重复了??
  if (typeof PluginArray !== "undefined") {
    plugins = navigator.plugins;
    if (plugins instanceof PluginArray);
  }
  /// 重复略
  typeof MSPluginsCollection !== "undefined";

  !!window["_phantom"];
  !!window["callPhantom"];
  // !window['__nightmare']; // 跟太快了，这行不确定是否执行
  mask |= +!window.Audio << 3;

  // ##
  // 测试以下4项
  typeof window.globalThis["global"] !== "undefined";
  typeof window.globalThis["process"] === "undefined";
  "object" === "undefined";
  mask |=
    +(typeof window.globalThis["__non_webpack_require__"] !== "undefined") << 4;

  loc_href = location.href;
  re1 = RegExp("^((file|https?):\\/\\/localhost)", "i");
  re2 = RegExp(
    "^https?:\\/\\/([0-9]{1,3}(\\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})"
  );
  // 顺序执行取最后的值
  re1.test(loc_href); // false
  re2.test(loc_href); // false
  document.location !== window.location; // false
  mask |= +(location.valueOf(location) /* false*/ !== location) << 5;

  // 在一轮测试
  // 通过user agent测试是什么系统
  navigator.userAgent;
  osObj = Object.defineProperty({}, "name", {
    value: "Other",
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  // 这里的fg_regs()全部是dy的vmp函数，这里无法具体化
  // 但是这些函数更确切说是变量是正则的集合
  osObj = Object.defineProperty(osObj, "isAndroid", {
    // 这些value对应正则表达式
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  osObj = Object.defineProperty(osObj, "isiOS", {
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  osObj = Object.defineProperty(osObj, "isLinux", {
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  osObj = Object.defineProperty(osObj, "isMacOS", {
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  osObj = Object.defineProperty(osObj, "isWindows", {
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  osObj = Object.defineProperty(osObj, "isHarmonyOS", {
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  osObj = Object.defineProperty(osObj, "isOther", {
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  // osObj生成的是去掉is，并且按照字母排序的类似数组的东西
  // 抽取其中一个举例：
  // { name: "HarmonyOS", regs:['/droid ([\w.]+)\b.+(harmonyos)/i', '/OpenHarmony/i']}

  if (typeof globalThis["Symbol"] != "undefined") {
    !!globalThis["Symbol"].iterator.values; // 测试是否存在
  }

  // 执行  s n f e
  globalThis["Symbol"].iterator.values.call(osObj); // ArrayItetator

  obj1 = Object.defineProperty({}, "s", {
    // 似乎执行它得到ArrayIterator
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });
  // 执行完变成：任意项举例
  // { done: false, value: {name: "HarmonyOS", regs:['/droid ([\w.]+)\b.+(harmonyos)/i', '/OpenHarmony/i']}}

  obj1 = Object.defineProperty(obj1, "n", {
    // 执行iterator.next， 得到HarmonyOS()
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });
  // 这是程序中非 s n f e代码，
  // 取它的变量done的值然后取非，在取value
  // 在取regs
  // regs.some函数， some函数执行regs.test(userAgent), 测试匹配每一个reg
  // 重复调用n函数，去测试前面声明的osObj中的value的regs
  // 最后匹配到Windows，然后更新osObj.name = "Windows"

  obj1 = Object.defineProperty(Object, "f", {
    // 执行globalThis['Symbol'].iterator.return 释放资源， 释放后判断是否为undefined， null == undefined 为true， 释放后判断是否为undefined
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  obj1 = Object.defineProperty(obj1, "e", {
    // 这个函数似乎没执行??
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  // 新一轮测试，测试是什么平台?
  navigator.Win32; // 可是没有定义, 后用的"Win32" ??
  platformObj = Object.defineProperty({}, "name", {
    value: "Other",
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  // 这里的fg_regs()全部是dy的vmp函数，这里无法具体化
  platformObj = Object.defineProperty(platformObj, "isAndroid", {
    // 这些value对应正则表达式
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  platformObj = Object.defineProperty(platformObj, "isApple", {
    // 这些value对应正则表达式
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  platformObj = Object.defineProperty(platformObj, "isLinux", {
    // 这些value对应正则表达式
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  platformObj = Object.defineProperty(platformObj, "isWindows", {
    // 这些value对应正则表达式
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  platformObj = Object.defineProperty(platformObj, "isOther", {
    // 这些value对应正则表达式
    value: fg_regs(),
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  if (typeof globalThis["Symbol"] != "undefined") {
    !!globalThis["Symbol"].iterator.values; // 测试是否存在
  }

  globalThis["Symbol"].iterator.values.call(platformObj);

  // 同样 定义 s n f e
  obj1 = Object.defineProperty({}, "s", {
    // 似乎执行它得到ArrayIterator
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  obj1 = Object.defineProperty(obj1, "n", {
    // 执行iterator.next
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });
  // 这是程序中非 s n f e代码，
  // 取它的变量done的值然后取非，在取value
  // 在取regs
  // regs.some函数， some函数执行regs.test("Win32"), 测试匹配每一个reg
  // 重复调用n函数，去测试前面声明的osObj中的value的regs
  // 最后匹配到Windows，然后更新platformObj.name = "Windows"

  // f函数取U[8] U[9]都是false，这两个没跟，不知道怎么得到的
  obj1 = Object.defineProperty(Object, "f", {
    // 执行globalThis['Symbol'].iterator.return 释放资源， 释放后判断是否为undefined， null == undefined 为true， 释放后判断是否为undefined
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  obj1 = Object.defineProperty(obj1, "e", {
    // 这个函数似乎没执行??
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  // 取osObj.isXXX
  osObj.isWindows; // 得到其对应的fg函数, 执行这个函数
  platformObj.isWindows; // 得到其对应的fg函数, 执行这个函数
  // 取platformObj.isXXX
  // 两个isWindows函数执行逻辑相同，都是下面这行代码; 其他相同，如isLinux返回Linux
  osObj.name === "Windows";
  platformObj.name === "Windows"; // true, 但还是对true取反，还是变成了false
  // 最终生成了5个false，逐个判断后无条件跳转了
  // 把最后一个false转换为数字， 即+false

  test_ret = [false, false, false, false, false];
  mask |= +test_ret[4] << 6;

  // 至此 U[16] = mask = 1，U[16]就是这样来的

  // ...
}

// 鼠标轨迹判断，防作弊
function envTrackU17() {
  // J132_get5_vr // 之前全局的json结构里面保存着aid，pageId等
  // 取其属性track得
  track = {
    mode: 0,
    delay: 300,
    paths: [],
  };
  track["mode"] !== 0; // false
  // J132_get6_N; 指令长度21，无参
  // 里面再次进入U18 vm， 指令长度82， 无参
  // J232_U 即为：
  // n :{ front:0,
  //   items:[{ // 长度为401，但没有第二项以后的元素
  //           "x": 211,
  //           "y": 1,
  //           "ts": 1763713216251
  //       }, {
  //           "x": 656,
  //           "y": 111,
  //           "ts": 1763713216369
  //       }],
  //     rear:2,
  //     trigger:2
  //  }
  // 大循环
  // 取n.data 不知道为什么是一个vm函数，长度为67，无参，然后进入这个函数
  // n.isEmpty() 这个还是vm函数，长度为8，无参
  // 循环1
  // n.front === n.rear // false， 取data，否则就是没有数据，返回
  // ta = []
  // front = n.front
  // ta.push(items[front])
  // front += 1;
  // 循环2
  // n.length() 是个vm函数，指令长度6，无参 逻辑是n.items.length 返回401
  // front %= n.items.length // 即 1 %= 401 值为1
  // rear = n.rear
  // 继续循环1的位置
  // front !== rear // true
  // ta.push(items[front])
  // front += 1;
  // 继续循环2的位置
  // n.length() 是个vm函数，指令长度6，无参 逻辑是n.items.length 返回401
  // 略
  // 在front !== rear比较时为false退出函数
  // 得到数组ta = [
  //     {
  //         "x": 211,
  //         "y": 1,
  //         "ts": 1763713216251
  //     },
  //     {
  //         "x": 656,
  //         "y": 111,
  //         "ts": 1763713216369
  //     }
  // ]
  // ta.length === 0
  // ta.length === 1
  // tv = 0
  // ta.reduce 是vm函数，长度为79
  // x = Math.pow([(ta[1].x - ta[0].x), 2])
  // y = Math.pow((ta[1].y - ta[0].y), 2])
  // rx = Math.sqrt(x)
  // ts = ta[1].ts - ta[0].ts;
  // rx = rx / ts;
  // tv += rx;
  // len = ta.length - 1;
  // tv = (tv / len) > 18; // false, 返回0
  // 进入U19 vm函数，长度24，无参
  // J232_get1_I

  // L1
  // 继续取n.data，走大循环逻辑
  // 此时n的items的前两项不见了，其他值均为0了，因此取data应该什么都没取到，返回一个空数组[]ta
  // ta.length 此时为0了，和0比较相等了，
  // 下面直接取的
  // v1 = 1, v2 = 2, v1 = v1 << v2 等于4，作为返回值

  // tv |= 4
  // 进入U20 vm函数，长度82，无参
  // J232_get3_M
  // 同上面L1逻辑
  // 但  v1 = 1, v2 = 3, v1 = v1 << v2 等于8，作为返回值

  // 在 tv |= 8, 最后调试时为12
  U17 = 12; // 普通为14
}

// 函数特征检测，某些函数的属性作为函数特征保存，而这些属性不用于运算逻辑
// 堆栈检测，是否包含非dy js文件、函数，特征等
// host检测，是否包含localhost，或者类似127.0.0.1这种数字host
function envDomPrintU37() {
  u37 = new Array();
  // triger map
  (J700Trigger = 0), (J700ItemsLength = 401);
  J700Trigger %= J700ItemsLength;
  J700Trigger %= 256;
  J700Trigger &= 255;

  (J770Trigger = 0), (J770ItemsLength = 101);
  J770Trigger %= J770ItemsLength;
  J770Trigger %= 256;
  J770Trigger &= 255;

  (J690Trigger = 0), (J690ItemsLength = 201);
  J690Trigger %= J690ItemsLength;
  J690Trigger %= 256;
  J690Trigger &= 255;

  (J132Trigger = 0), (J132ItemsLength = 101);
  J132Trigger %= J132ItemsLength;
  J132Trigger %= 256;
  J132Trigger &= 255;

  u37[0] = J700Trigger; // 0
  u37[1] = J770Trigger; // 0
  u37[2] = J690Trigger; // 0
  u37[3] = J132Trigger; // 0

  if (programVersion === G_DEBUG) {
    console.log("xxxxxxxxxx::: ", u37);
  }

  _u2 = u37; // 调试跟踪时对应执行时栈U2

  m = Array.isArray.apply(Array, [u37]); // 程序返回true，是一个数组对象

  if (programVersion === G_DEBUG) {
    console.log("xxxxxxxxxx::: ", m);
  }

  _u3 = u37; // 调试跟踪时对应执行时栈U3
  _u4 = u37[0]; // 调试跟踪时对应执行时栈U4
  _u5 = u37[1]; // 调试跟踪时对应执行时栈U5
  _u6 = u37[2]; // 调试跟踪时对应执行时栈U6
  _u7 = u37[3]; // 调试跟踪时对应执行时栈U7

  (v0 = document.all), (compareV = undefined);
  v0 = v0 === compareV; // 结果为false
  compareV = null;
  v1 = v0 === compareV; // 结果为false
  v2 = document.all.__proto__;
  v3 = HTMLAllCollection;
  compareV = v3.prototype;
  v2 = v2 !== compareV; // 结果为false

  v4 = document.all.toString.apply(document.all, []);

  v3 = v3 !== v4; // 结果为false, 但我这里不知道哪里拉下来，总返回true

  v4 = document["all"];
  v5 = document["all"];
  v4 = v4 !== v5;

  docEnvTest = [v0, v1, v2, !v3, v4]; // 这里v3是false, 这里手动改一下 !v3

  // 如果检测到，某些逻辑就不走了，下面代码是受影响的逻辑，v[p]即计算的false或者true，否则继续
  // 其他包括上述都同理
  // 31 === t ? (U = o[a++], v[p] ? a += U : --p) : (w = v[p--], v[p] = v[p] < w)
  if (programVersion === G_DEBUG) {
    console.log("xxxxxxxxxx::: ", docEnvTest);
  }

  docEnvTest = docEnvTest.filter((val) => {
    return val === true; // 这里实际会触发执行vmp函数
  });

  if (!docEnvTest.length) {
    // 即没有true的选项
    _u8 = false;
  }

  pattern1 = [RegExp, "^((file|https?):\\/\\/localhost)", "i"];
  G = [void 0, pattern1[1], pattern1[2]];
  reg1 = new (Function.bind.apply(pattern1[0], G))();

  pattern2 = [
    RegExp,
    "^https?:\\/\\/([0-9]{1,3}(\\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})",
    "i",
  ];
  G = [void 0, pattern2[1], pattern2[2]];
  reg2 = new (Function.bind.apply(pattern2[0], G))();

  pattern3 = [
    RegExp,
    "^(Module._compile|Object.Module|Module.load|Function.Module._load)",
    "i",
  ];
  G = [void 0, pattern3[1], pattern3[2]];
  reg3 = new (Function.bind.apply(pattern3[0], G))();
  if (programVersion === G_DEBUG) {
    console.log("xxxxxxxxxx ::: ", reg1, reg2, reg3);
  }
  // Error: Fail to construct 'URL': Invalid URL
  // at d (https://lf-c-flwb.bytetos.com/obj/rc-client-security/web/stable/1.0.1.19-fix.01/bdms.js:2:131912)
  // at X (https://lf-c-flwb.bytetos.com/obj/rc-client-security/web/stable/1.0.1.19-fix.01/bdms.js:2:131083)
  // at XMLHttpRequest.n (https://lf-c-flwb.bytetos.com/obj/rc-client-security/web/stable/1.0.1.19-fix.01/bdms.js:2:130952)
  // at XMLHttpRequest.<anonymous> (https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:306874)
  // at XMLHttpRequest.l [as send] (https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:299704)
  // at https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:345123
  // at new Promise (<anonymous>)
  // at https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:343014
  // at https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:349566
  // at l.request (https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:347911)

  // 类似上面的错误
  // 下面这行代码，我执行不了
  //err = Error.apply(void 0, "Fail to construct 'URL': Invalid URL");
  // 不过它等价于下面代码，都是在当前抛出一个异常
  err = new Error("Fail to construct 'URL': Invalid URL");

  var isErrNull = false;
  if (err === undefined || err === 0) {
    isErrNull = true;
  }
  // estack = err.toString().split("\n"); // 这里执行不了，就换一下，模拟一下代码吧
  estack =
    "Error: Fail to construct 'URL': Invalid URL\n " +
    "at d (https://lf-c-flwb.bytetos.com/obj/rc-client-security/web/stable/1.0.1.19-fix.01/bdms.js:2:131912)\n " +
    "at X (https://lf-c-flwb.bytetos.com/obj/rc-client-security/web/stable/1.0.1.19-fix.01/bdms.js:2:131083)\n " +
    "at XMLHttpRequest.n (https://lf-c-flwb.bytetos.com/obj/rc-client-security/web/stable/1.0.1.19-fix.01/bdms.js:2:130952)\n " +
    "at XMLHttpRequest.<anonymous> (https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:306874)\n " +
    "at XMLHttpRequest.l [as send] (https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:299704)\n " +
    "at https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:345123\n " +
    "at new Promise (<anonymous>)\n " +
    "at https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:343014\n " +
    "at https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:349566\n " +
    "at l.request (https://lf-webcast-platform.bytetos.com/obj/webcast-platform-cdn/webcast/douyin_live/9417.f277fc75.js:4552:347911)";

  if (programVersion === G_DEBUG) {
    console.log("xxxxxxxxxx::: ", estack);
  }
  estack = estack.split("\n");
  if (programVersion === G_DEBUG) {
    console.log("xxxxxxxxxx::: ", estack);
  }

  for (var i = 0; i < estack.length; i++) {
    test = estack[i];
    testUrl1 = reg1.test(test);
    testUrl2 = reg2.test(test);
    testUrl3 = reg3.test(test);
    if (programVersion === G_DEBUG) {
      console.log("xxxxxxxxxx::: ", testUrl1, testUrl2, testUrl3);
    }
  }
  // 返回值是false
  errEnvTest = [isErrNull, testUrl1, testUrl2, testUrl3];

  screenEvn = [false, false, false];
  var screen = Object.getOwnPropertyDescriptor.apply(Object, [
    window,
    "screen",
  ]);
  val = screen.value;
  val = !!val;

  var scrLen = Object.keys.apply(Object, [window.screen]).length; // 结果为[]

  if (screen === null || screen === undefined || val || scrLen != 0) {
    // 应该是Error, 检测
    screenEvn[0] = true;
  }

  var scr = window.screen;
  if (scr === null || screen === undefined) {
    // err
    screenEvn[1] = true;
  }

  scr = scr.__proto__;
  keys = Object.keys.apply(Object, [scr]);
  if (keys.length === 0) {
    // err
    screenEvn[2] = true;
  }

  var screenEnvTest = screenEvn.filter((val) => {
    return val === true;
  });

  if (screenEnvTest > 0) {
    // err
  }

  windowEnv = !!window.cefSharp; // 检测
  windowEnv = !!window.window; // // 检测
  windowEnv = !!window.eoapi; // // 检测
  windowEnv = !!window.eoWebBroserDispatcher; // // 检测

  // 调试时这个大对象对应U5
  objp = Object.defineProperty({}, "name", {
    value: "Other",
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  // 这里的fg_regs()全部是dy的vmp函数，这里无法具体化
  objp = Object.defineProperty(objp, "isHuaWei", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  objp = Object.defineProperty(objp, "isOpera", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  objp = Object.defineProperty(objp, "isFirefox", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  objp = Object.defineProperty(objp, "isEdge", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  objp = Object.defineProperty(objp, "isIE", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  objp = Object.defineProperty(objp, "isChrome", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  objp = Object.defineProperty(objp, "isSafari", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  objp = Object.defineProperty(objp, "isOther", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  obj1 = Object.defineProperty(Object, "s", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  obj1 = Object.defineProperty(Object, "n", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  obj1 = Object.defineProperty(Object, "f", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  obj1 = Object.defineProperty(Object, "e", {
    value: "", //fg_regs()
    writable: !0,
    configurable: !0,
    enumerable: !0,
  });

  // 先后执行 s, n, e, f
  // 匹配浏览器的正则，可能一个浏览器有多个正则
  // 先匹配的HuaWei，后匹配Chrome成功, 最后填充这个对象的name为Chrome
  exRegs = [];

  // 这些都粗略跑过了，具体逻辑不还原了，很繁琐，频繁调用vmp函数
  // 在调用isXXXX方法取和name做对比，isXXXX方法返回的是对应浏览器的名字
  isHuaWei = _u9 = false; // _u9
  isHuaWeiS = _u10 = false; // _u10
  isHuaWeiN = _u11 = false; // _u11
  isHuaWeiE = _u12 = false; // _u12
  isHuaWeiF = _u13 = false; // _u13
  isChrome = _u14 = true; // _u14
  // 略

  _u15 = +_u14;
  _u15 = (+_u8 << 1) | +_u15;
  _u15 = (+_u9 << 2) | +_u15;
  _u15 = (+_u10 << 3) | +_u15;
  _u15 = (+_u11 << 4) | +_u15;
  _u15 = (+_u12 << 5) | +_u15;
  _u15 = (+_u13 << 6) | +_u15;
  _u15 = (+_u14 << 7) | +_u15; // 1 << 7 = 128, 128 | 1 = 129, 最后_u15为129

  // 其实得到如下的数据
  // 1: [0,0,0,0] ; 对应上面_u2
  // 2: [0,0,0,0] ; 对应上面_u3
  // 3:0, 4:0, 5:0, 6:0 ; 对应上面_u4 - _u7
  // 7 - 13 均为false ; 对应上面的_u8 - _u14

  // 这里是取_u4 - _u4，然后再concat上_u15，即得到[0,0,0,0,129]
  U37 = u37.concat([_u15]);

  console.log("U37 ::::: ", U37);
  return U37;
}

function _random(inObj) {
  inval = inObj["0"];
  inval1 = inObj["1"];

  in0 = inval[0];
  in1 = inval[1];

  flag = 0;
  r = Math.random() * 65535;

  r1 = r & 255;
  r2 = (r >> 8) & 255;

  if (inObj["length"] > 1) {
    if (inval1 != void 0) {
      flag = inObj["1"];
    }
  } else {
    flag = 0;
  }

  if (flag === 1) {
    r2 = (Math.random * 40) >> 0;
  }

  if (flag === 2) {
    // 忽略环境检测
    r1 = (Math.random() * 240) >> 0;
    if (r1 > 109) {
      r1 = r1 + (r1 % 2);
      r1 += 1;
    }

    // 忽略环境匹配
    r2 = ((Math.random * 255) >> 0) & 77;

    // 调试时发现如果关于类似navigator.pemrissions["microphone"] === "granted"不匹配
    // r2不进行任何操作直接返回
    // 下面代码是命中的情况，因为实际情况应该就是命中的
    r2 |= 1 << 1;
    r2 |= 1 << 4;
    r2 |= 1 << 5;
    r2 |= 1 << 7;
  }

  u1 = (r1 & 170) | (in0 & 85);
  u2 = (r1 & 85) | (in0 & 170);
  u3 = (r2 & 170) | (in1 & 85);
  u4 = (r2 & 85) | (in1 & 170);
  return [u1, u2, u3, u4];
}


function decode_transform(narray) {
    const E1 = 145, E2 = 110, E3 = 66, E4 = 189, E5 = 44, E6 = 211;
    let array = [];

    let i = 0;

    // 处理完整的 4 字节块（对应原来的 3 字节）
    while (i + 3 < narray.length) {
        let a = narray[i];
        let b = narray[i + 1];
        let c = narray[i + 2];
        let d = narray[i + 3];

        // 关键逆公式（由 E1~E6 组合恢复 A0,A1,A2）
        let A0 = (a & E2) | (d & E1);
        let A1 = (b & E4) | (d & E3);
        let A2 = (c & E6) | (d & E5);

        array.push(A0, A1, A2);
        i += 4;
    }

    // 剩余尾部（1 或 2 字节）
    // 你的尾部逻辑是：
    // push(array[i]);
    // if (array[i+1] != undefined) push(array[i+1]);
    //
    // 所以逆向需要按 1:1 拆回
    let remain = narray.length - i;

    if (remain === 1) {
        array.push(narray[i]);
    } else if (remain === 2) {
        array.push(narray[i], narray[i + 1]);
    }

    return array;
}


/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @Author: AngelToms
 * ab算法实现，生成a_bogus
 */

// 先测试是否包含a_bogus，不存在依次执行下列动作
G_DEBUG = "debug";
G_RELEASE = "release";
programVersion = G_DEBUG;

// 模拟环境检测
EnvTestTurnOn = false;
// 它是一个性能API方法，返回一个DOMHighResTimeStamp（代表 "高分辨率时间戳"），简单地说，这是一个以毫秒为单位的时间值，
// 在两个时间点之间的小数部分。
// performance.now()最常见的用例是监控一段代码的执行时间。现实生活中的用例可以包括视频、音频、游戏和其他媒体的基准测试和监控性能。
// 模拟执行performance
// 不需要了，已去掉了在创建U数组中的代码
PerformanceTestTurnOn = false;
// 刚刚进入页面时取时间戳enterPageTs
// 这个时间戳是判断a_bogus是否存在后，不存在马上取的时间戳，最早的时间戳
enterPageTs = +Date.now();

var AB_ARRAY_SIZE = 256;
var DY_SALT = "dhzx";
// 用于生成a_bogus的大数组
U = [];

var sdkVersion = "1.0.1.19-fix.01";
var aid = 6383;
var pageId = 7571;

onwheelx = {
    "value": "0X21",
    "writable": false,
    "enumerable": true,
    "configurable": true
};

innerScreen = {
    "innerWidth": 2048, // 会判断是否是800
    "innerHeight": 960, // 会判断是否是600
    "outerWidth": 2554,
    "outerHeight": 1386,
    "availWidth": 2560,
    "availHeight": 1392,
    "sizeWidth": 2560,
    "sizeHeight": 1440,
    "platform": "Win32"
};

// 随机函数+数组变化
// 这个数组存在问题或者更新了，用后面那个算法
// function random(inval) {
//     in0 = inval[0];
//     in1 = inval[1];

//     r = Math.random() * 65535;

//     u1 = (r & 255 & 170) | (in0 & 85);
//     u2 = (r & 255 & 85) | (in0 & 170);
//     u3 = (((r >> 8) & 255) & 170) | (in1 & 85);
//     u4 = (((r >> 8) & 255) & 85) | (in1 & 170);

//     if (programVersion === G_DEBUG) {
//         console.log("random :: ", [u1, u2, u3, u4]);
//     }
//     return [u1, u2, u3, u4];
// }


function random(inObj) {
  inval = inObj["0"];
  inval1 = inObj["1"];

  in0 = inval[0];
  in1 = inval[1];

  flag = 0;
  r = Math.random() * 65535;

  r1 = r & 255;
  r2 = (r >> 8) & 255;

  if (inObj["length"] > 1) {
    if (inval1 != void 0) {
      flag = inObj["1"];
    }
  } else {
    flag = 0;
  }

  if (flag === 1) {
    r2 = (Math.random * 40) >> 0;
  }

  if (flag === 2) {
    // 忽略环境检测
    r1 = (Math.random() * 240) >> 0;
    if (r1 > 109) {
      r1 = r1 + (r1 % 2);
      r1 += 1;
    }

    // 忽略环境匹配
    r2 = ((Math.random * 255) >> 0) & 77;

    // 调试时发现如果关于类似navigator.pemrissions["microphone"] === "granted"不匹配
    // r2不进行任何操作直接返回
    // 下面代码是命中的情况，因为实际情况应该就是命中的
    r2 |= 1 << 1;
    r2 |= 1 << 4;
    r2 |= 1 << 5;
    r2 |= 1 << 7;
  }

  u1 = (r1 & 170) | (in0 & 85);
  u2 = (r1 & 85) | (in0 & 170);
  u3 = (r2 & 170) | (in1 & 85);
  u4 = (r2 & 85) | (in1 & 170);
  return [u1, u2, u3, u4];
}

function getTxUri(url, method) {
    if (method == 'post') {
        return url + DY_SALT;
    } else {
        return "" + DY_SALT;
    }
}

// 以下代码等价于  var keyArray = String.fromCharCode(0.00390625, 1, 14);
// ## 调试时发现无区别的刷新，某些请求会以以下为key：
// [0.00390625, 1, 0] 、[0.00390625, 1, 12]
// key的出现次序是: [0.00390625, 1, 0] 、[0.00390625, 1, 12]、[0.00390625, 1, 14]
function createKey() {
    var keyArray = [];

    var magic = 1;
    // 即1/256 = 0.00390625
    magic /= AB_ARRAY_SIZE;
    keyArray.push(magic);
    // 即1%256
    magic = 1;
    magic %= AB_ARRAY_SIZE;
    keyArray.push(magic);
    // 即14%256
    magic = 14;
    magic %= AB_ARRAY_SIZE;
    keyArray.push(magic);

    return String.fromCharCode.apply(String, keyArray);
};

// 逆向得到的，魔改了部分的rc4
function dyRc4(key, text) {
    // 初始化 S-Box（0~255）
    var S = new Uint8Array(AB_ARRAY_SIZE);
    var K = new Uint8Array(AB_ARRAY_SIZE);

    var maxSIndex = AB_ARRAY_SIZE - 1;
    for (var i = 0; i < AB_ARRAY_SIZE; i++) {
        // 这里改动了一下，这里是初始化Sbox
        S[i] = maxSIndex - i;
        K[i] = key.charCodeAt(i % key.length);
    }

    if (programVersion === G_DEBUG) {
        console.log("rc4 init :: ", S);
    }

    // 这里改动了一下，这里是密钥变化轮
    var j = 0;
    for (var i = 0; i < AB_ARRAY_SIZE; i++) {
        j = (j * S[i] + j + K[i]) % AB_ARRAY_SIZE;
        [S[i], S[j]] = [S[j], S[i]]; // 交换 S[i] 和 S[j]
    }

    if (programVersion === G_DEBUG) {
        console.log("rc4 ex :: ", S);
    }

    // 伪随机数生成算法（PRGA）
    var i = 0, k = 0;
    var cipher = new Uint8Array(text.length);
    // 新变量
    var ucipher = "";

    for (let n = 0; n < text.length; n++) {
        i = (i + 1) % AB_ARRAY_SIZE;
        k = (k + S[i]) % AB_ARRAY_SIZE;
        [S[i], S[k]] = [S[k], S[i]]; // 交换 S[i] 和 S[k]

        let rnd = S[(S[i] + S[k]) % AB_ARRAY_SIZE]; // 生成密钥流字节
        cipher[n] = text.charCodeAt(n) ^ rnd; // XOR 操作

        // 这里是变化的地方
        ucipher += String.fromCharCode(cipher[n]);
    }

    if (programVersion === G_DEBUG) {
        console.log("rc4 degist :: ", cipher);
        console.log("rc4 ucode :: ", ucipher);
    }

    return ucipher;
}


// 标准的base64实现和这个存在区别，标准的base64以每3个字符为一个单位进行处理，但逆向的逻辑和标准算法实现是一致的
function dyBase64(ucode, mindex, pad) {
    if (pad === null) {
        pad = "=";
    }

    var letter0 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var letter1 = "Dkdpgh4ZKsQB80/Mfvw36XI1R25+WUAlEi7NLboqYTOPuzmFjJnryx9HVGcaStCe=";
    var letter2 = "Dkdpgh4ZKsQB80/Mfvw36XI1R25-WUAlEi7NLboqYTOPuzmFjJnryx9HVGcaStCe=";
    var letter3 = "ckdp1h4ZKsUB80/Mfvw36XIgR25+WQAlEi7NLboqYTOPuzmFjJnryx9HVGDaStCe";
    var letter4 = "Dkdpgh2ZmsQB80/MfvV36XI1R45-WUAlEixNLwoqYTOPuzKFjJnry79HbGcaStCe";

    var mapTable = {};
    mapTable["s0"] = letter0;
    mapTable["s1"] = letter1;
    mapTable["s2"] = letter2;
    mapTable["s3"] = letter3;
    mapTable["s4"] = letter4;

    var uaTable = mapTable[mindex];

    if (programVersion === G_DEBUG) {
        console.log("UAE code length: ", ucode.length);
        console.log("UAE map table: ", uaTable);
    }

    var uaEncode = "";
    var i = 0;
    // var step = 3;
    while (i < ucode.length) {
        var c1 = ucode.charCodeAt(i++);
        var c2 = _c2 = ucode.charCodeAt(i++);
        var c3 = _c3 = ucode.charCodeAt(i++);

        c1 = (c1 & 255) << 16;

        if (isNaN(_c2)) {
            c2 = 0;
        } else {
            c2 = (c2 & 255) << 8;
        }

        if (isNaN(_c3)) {
            c3 = 0;
        } else {
            c3 = (c3 & 255);
        }

        c = (c1 | c2) | c3;

        var ch1 = (c & 16515072) >> 18;
        var ch2 = (c & 258048) >> 12;
        var ch3 = (c & 4032) >> 6;
        var ch4 = (c & 63);

        uaEncode += uaTable.charAt(ch1);
        uaEncode += uaTable.charAt(ch2);

        if (!isNaN(_c2)) {
            uaEncode += uaTable.charAt(ch3);
        }

        if (!isNaN(_c3)) {
            uaEncode += uaTable.charAt(ch4);
        }

        if (isNaN(_c2)) {
            uaEncode += pad;
        }

        if (isNaN(_c3)) {
            uaEncode += pad;
        }

        if (programVersion === G_DEBUG) {
            console.log("UAE step origin val :: ", c1, c2, c3, c, ch1, ch2, ch3, ch4);
            console.log("UAE step encode val :: ", uaEncode);
        }
    }

    return uaEncode;
}

function makeVersionArray(ver) {
    var verArray = [];
    var array = ver.split(".");
    verArray.length = array.length;
    for (var i = 0; i < array.length; i++) {
        array[i] = ~array[i];
        array[i] = ~array[i];
        verArray[i] = array[i];
    }
    if (programVersion === G_DEBUG) {
        console.log("sdk version :: ", verArray);
    }
    return verArray;
}

// 对U88数组进行转换，后面函数用，放在下面函数前面声明
function u88Transform(array) {
    narray = [];
    var i = 0;
    var E1 = 145, E2 = 110, E3 = 66, E4 = 189, E5 = 44, E6 = 211;
    while (i < array.length) {
        if ((i + 2) < array.length) {
            var x = (Math.random() * 1000) & 255; // 有随机数，不可逆，即使随机数只有 0 - 255
            a = (x & E1) | (array[i] & E2);
            b = (x & E3) | (array[i + 1] & E4);
            c = (x & E5) | (array[i + 2] & E6);
            d = (array[i] & E1) | (array[i + 1] & E3) | (array[i + 2] & E5);

            narray.push(a);
            narray.push(b);
            narray.push(c);
            narray.push(d);
        }
        else {
            narray.push(array[i]);
            if (array[i + 1] != void 0) {
                    narray.push(array[i + 1]);
            } 
        }
        i += 3;
    }

    if (programVersion === G_DEBUG) {
        console.log("new magic array :: ", narray);
    }

    return narray;
}


// 先测试是否包含a_bogus，不存在依次执行下列动作
// 测试执行的逻辑在此行代码: n.apply(d, e), 因此在此处下条件断点(e == 'a_bogus')
// 执行结果 v[++p] = m(false), 继续执行方法逻辑
// 这里面步骤每一步都非常关键，前面步骤是后续生成大数组的基础数据
// 非DEBUG模式只需要输入参数uri，其他参数为0即可
// uri格式：uri = "device_platform=webapp&aid=6383&channel=channel_pc_web&update_version_code=170400&pc_client_type=1&pc_libra_divert=Windows&support_h265=1&support_dash=1&version_code=170400&version_name=17.4.0&cookie_enabled=true&screen_width=2560&screen_height=1440&browser_language=zh-CN&browser_platform=Win32&browser_name=Chrome&browser_version=135.0.0.0&browser_online=true&engine_name=Blink&engine_version=135.0.0.0&os_name=Windows&os_version=10&cpu_core_num=20&device_memory=8&platform=PC&downlink=0.55&effective_type=3g&round_trip_time=500&webid=7482403095261316617&uifid=c4a29131752d59acb78af076c3dbdd52744118e38e80b4b96439ef1e20799db0e06c5be9532d94f0b7725e552b444440ba2c1788bfe5c8a70c5c57ab949d98c92463baa114bc91665f6b6f521e7d9a95ab08e303534db5095aeb069825e1f5e2bf4a4d0f4046a281efabce92529f452a210f0f0177b5e702cbf977dbc27d98980ac8f88aee8c3a95a21b96af00089db82838edad8a54dca3f57bbbc31132d6bed21846d5a9268a6f4485626786e98bd17c3347439baef5c777c1c3db74544de5&msToken=FWM9NpNgsrUHOtqnF7YmwQU9Y3hIIZOuTf65COJG4V8VyNeaU89328CcMbM_rImYIwdj2AJKjX9CZT_tbM4HxaObL5GW2izSWid3HFUiZYSeu-jIt1om8zSvQAO4Ztm2zIFSfGvtX6Hjq_zZCCxcb2lWeyJbOaRD5TZ5mE7aYo0xjPwUsNmKX7s%3Ddhzx";
// DEBUG模式需要参数ts
// DEBUG模式下，这些参数是在调试时从浏览器watch中获得的变量
// 此目的可以用于观察对比很多变量是否和浏览器生成一致
// 其他变量就自由生成了，因为很多变量是经过random的，无法达到和浏览器输出同样的值
// 但算法没有问题，服务端应该可以通过验证
// 程序中很多值是经过一些列检测计算得到的，虽在程序中写死，但不影响最终结果，因为这些值是在真实环境中一步一步跟踪下来得到的值
function makeABogus(uri, ts) {

    if (programVersion === G_DEBUG && ts !== 0) {
        enterPageTs = ts;
    }

    if (programVersion === G_DEBUG) {
        console.log("enter page ts :: ", enterPageTs);
    }

    // 调试时，进入ab算法真正的开始是U[12]=11，后续才走下面的逻辑

    // 刚刚进入页面时，时取时间戳enterPageTs
    // 之后在创建大数组U时，创造第一个U[0]时，U[0]是一个包含22项元素的数组，其中U[0][2] = "dhzx",
    // U[0][3] 就是 Date.now() - 1, 即下面的代码_ink
    // 通过Object.defineProperty定义了ink，如下：
    // ink_obj = {};
    // var dnow = Date.now(); // 1753084107074
    // dsub = dnow - 1;
    // // 定义ink，即 {"ink": 1753084107073 }
    // 但是源码是通过如下形式定义的
    // Object.defineProperty(ink_obj, "ink", {
    //         value: dsub,
    //         writable: !0,
    //         configurable: !0,
    //         enumerable: !0
    // }); 
    // 这里略
    // 这里用下面等价代码实现{"ink": 1753084107073 }
    _ink = +Date.now() - 1;
    // 即navigator.ink
    navigator.vendorSubs = { ink: _ink }; // 实际这里不需要定义，直接用_ink的值就行
    ink = navigator.vendorSubs.ink;


    // U0 和 U1 实际对应两个Object
    // U0创建时间早于U1
    // U1是正式进入生成ab算法时传递的参数

    // U[0] 是一个长度为22的数组，U[0][0 - 4]项如下所列
    // U[0][5] 是判断浏览器封装, 结构如下： {
    //     "name": "Chrome",
    //     "isChrome": f,
    //     "isEdge": f,
    //     "isFirefox": f,
    //     "isHuawei": f,
    //     "isIE": f,
    //     "isOpera": f,
    //     "isOther": f,
    //     "isSafari": f
    // }
    // U[0][6 - 21] 是指向虚拟机入口X的函数
    U[0] = [];
    U[0].length = 22;
    U[0][0] = {};
    U[0][1] = { "length": 0 };
    U[0][2] = DY_SALT;
    U[0][3] = enterPageTs;
    U[0][4] = 1; // 开始值为0，正式进入ab生成vm函数时会++，后面这个值还会增加，到ab生成完值为36
    // U[0][5] 后略

    // 没有用到, 通过跟踪程序，U[1]大多数据都是写死的，其他值是外部数据是固定的(如pageId, aid, sdkversion, uri，useragent等)
    // 而U[2 - 10] 来源于此
    U[1] = [];
    U[1].length = 9;
    U[1][0] = 1;
    U[1][1] = 0;
    U[1][2] = 8;
    U[1][3] = uri;
    U[1][4] = "";
    U[1][5] = navigator.userAgent;
    U[1][6] = pageId;
    U[1][7] = aid;
    U[1][8] = sdkVersion;

    // 写死，在进入函数前直接取的，作为参数传入
    U[2] = 1;
    U[3] = 0;
    U[4] = 8;

    if (uri.endsWith(DY_SALT)) {
        U[5] = uri;
    } else {
        U[5] = uri + DY_SALT;
    }

    U[6] = "";
    U[7] = navigator.userAgent.trim();
    U[8] = pageId;
    U[9] = aid;
    U[10] = sdkVersion;
    // 中间一系列 环境检测、 performance操作略
    // uri + dhzx, dhzx 两遍sm3后
    // bdms.js文件 key: "sum", value: function(t, r) 处下断点
    var urlSm3Array = sm3DigestTwice(U[5]);
    haltSm3Array = sm3DigestTwice(U[0][2]);
    var uaCrypt = dyRc4(createKey(), U[7]);

    uaEncode = dyBase64(uaCrypt, "s3", null);
    uaeSm3Array = sm3Digest(uaEncode);

    // 调试跟踪时，这个值是后面被赋值的
    // U21算完之后，U11才被赋值为navigator.vendorSubs
    // 接着判断U11.ink不为null和undefined后
    // 把U11.ink赋值给U22
    U[11] = navigator.vendorSubs;

    // 在刚刚进入ab生成算法时，U12是被值为11的
    // 后面对U13做了检测, 看U13, 没命中把U12赋值为3
    U[12] = 3;

    // 调试时实际它走的这样的代码，但是我们程序是构建的，因此就像下面那样赋值
    // 先测试window.onwheelx是否存在，再测试window.onwheelx['_Ax']是否存在
    // if (window.onwheelx) {
    //     if (window.onwheelx['_Ax']) {
    //         U13 = Object.getOwnPropertyDescriptor(window.onwheelx, "_Ax");
    //     }
    // }
    window.onwheelx = onwheelx;
    U[13] = window.onwheelx;
    // 调试时代码实际对其作了如下验证，通过验证U12被赋值为3，否则应该还是11
    // 我们直接对U12赋值为3了，因此下面代码忽略，但保留注释
    // is_hit_u13 = (U[13]  === null);
    // if (!is_hit_u13) {
    //     is_hit_u13 = (U[13]  === (void 0));
    // }
    // if (!is_hit_u13) {
    //     is_hit_u13 = (U[13] ["writable"]);
    // }
    // if (is_hit_u13 === false) {
    //     U[12] = 3;
    // } else {
    //     U[12] = 11; // 不变
    // }

    // 这里又取时间
    U[14] = +Date.now();

    // U[15]
    // t = {
    //     "reg": [
    //         1937774191,
    //         1226093241,
    //         388252375,
    //         3666478592,
    //         2842636476,
    //         372324522,
    //         3817729613,
    //         2969243214
    //     ],
    //     "chunk": [],
    //     "size": 0
    // }
    U[15] = []; // sm3算法魔术和chunk, 无用, 这里无需赋值，如果赋值则为t
    // U16是做了一系列的环境检测后计算得到的值，但是如果没有匹配任何计算的值就是1
    // 一共做了7个大类别的检测，对每种检测结果，按顺序 << (n - 1), 最后把这些结果|上上一次的值，第一次|1
    // 上面的意思是，举例说明：
    // 如果当前检测第1种，没匹配就为false，通过+false把其转换为0， 然后0 << (1-1)即 0 << 0 = 0, 然后0|1 = 1;
    // 我们这里称其为 env_mask = 0 | 1; 假设都没命中，都为false，取值都为0
    // 以此类推得：env_mask = ((0 << 0) | 1) | (0 << 1) | (0 << 2) | (0 << 3) | (0 << 4) | (0 << 5) | (0 << 6)
    // 最终值为1
    U[16] = 1; // 这里写死, 推导过程见上面注释

    // 生成类似creatUxx，都是做一些检测得到之后在组合计算
    // J132(vr -> N)
    // 执行取track.mode
    // J232(u -> I -> M)
    // 后面三个函数均取对应Object.data, rear 取值不为0存入新数组[]
    // 但三个取值均为0
    // 第一次执行 v1 = 1 << 1
    // 第二次 v2 = (2 << 1 | v1)
    // 第三次 v3 = (3 << 1) | v2
    // 最后生成值14
    // 这个我们无法干预，就只能目前写死14
    // 但我的一次调试时为12， 这个12不确定是否影响算法，我们就取14
    U[17] = 14;
    U[18] = urlSm3Array;
    U[19] = haltSm3Array;
    U[20] = uaEncode;
    U[21] = uaeSm3Array;
    // 出处见U11
    U[22] = ink;
    // 调试时发现是直接取值，写死的
    U[23] = [3, 82];
    // 也是写死的，直接取值的
    // 评论代码是magic
    U[24] = 41;
    U[25] = makeVersionArray(sdkVersion);

    var G1 = [void 0];
    var z = Date;
    var utc = +"1721836800000";
    // 源代码这样创建了一个数组，空的，3个元素
    var gDate = new (Function.bind.apply(z, G1));
    var gNow = gDate.getTime();
    U[26] = ((gNow - utc) / 1000 / 60 / 60 / 24 / 14) >> 0; // >> 0  移除小数点
    U[27] = 6;

    // 这里用到了enter page ts，仅这里用到了
    U[28] = (U[14] - U[0][3] + 3) & 255;
    U[29] = U[14] & 255;
    U[30] = (U[14] >> 8) & 255;
    U[31] = (U[14] >> 16) & 255;
    U[32] = (U[14] >> 24) & 255;
    U[33] = (U[14] / 256 / 256 / 256 / 256) & 255;
    U[34] = (U[14] / 256 / 256 / 256 / 256 / 256) & 255;
    U[35] = (U[16] % 256) & 255;
    U[36] = (U[16] / 256) & 255;

    // 慢速调试时得[34, 0, 0, 0, 129];
    // 数组中的129是直接取的，其他值是经过了一些所谓的环境计算判断，但无所谓
    // 实际可以为任意值，数组长度也不一定，我这个版本是[0, 0, 0, 0, 129]，一直跑不会出现问题
    // U[37] = creatUxx(); // 返回结果同下
    // 这里就取[0, 0, 0, 0, 129]
    U[37] = [0, 0, 0, 0, 129]; 
    U[38] = U[37][4] & 255;
    U[39] = (U[37][4] >> 8) & 255;
    U[40] = U[37][0];
    U[41] = U[37][1];
    U[42] = U[37][2];
    U[43] = U[37][3];
    U[44] = U[17] & 255;
    U[45] = (U[17] >> 8) & 255;
    U[46] = (U[17] >> 16) & 255;
    U[47] = (U[17] >> 24) & 255;
    U[48] = U[18][9];
    U[49] = U[18][18];
    U[50] = 3;
    U[51] = U[18][U[50]];
    // 这里应该包含一个分支用于计算U[52]的另一种情况
    u51 = U[51];
    // if (u51 != 11) { // u51 === 11 = false, 但也没见用
    //     u51 = U[37][4];
    //     u51 &= 2;
    // }

    U[52] = U[19][10];
    U[53] = U[19][19];
    U[54] = 4;
    U[55] = U[19][U[54]];
    u55 = U[55];
    // if (u55 != 8) { // u55 === 8 = false，但也没见用
    //     u55 = U[37][4];
    //     u55 &= 4;
    // }

    U[56] = U[21][11];
    U[57] = U[21][21];
    U[58] = 5;
    U[59] = U[21][U[58]];
    u59 = U[59];
    // if (u59 != 12) { // u59 === 12 = false，但也没见用
    //     u59 = U[37][4];
    //     u59 &= 8;
    // }

    U[60] = U[22] & 255;
    U[61] = (U[22] >> 8) & 255;
    U[62] = (U[22] >> 16) & 255;
    U[63] = (U[22] >> 24) & 255;
    U[64] = (U[22] / 256 / 256 / 256 / 256) & 255;
    U[65] = (U[22] / 256 / 256 / 256 / 256 / 256) & 255;
    U[66] = U[12];
    U[67] = U[8] & 255;
    U[68] = (U[8] >> 8) & 255;
    U[69] = (U[8] >> 16) & 255;
    U[70] = (U[8] >> 24) & 255;
    U[71] = U[9] & 255;
    U[72] = (U[9] >> 8) & 255;
    U[73] = (U[9] >> 16) & 255;
    U[74] = (U[9] >> 24) & 255;

    // 下面数据可以动态取，也可以用全局的innerScreen
    // innerScreen是写死的，但是从调试时获取的，不影响结果
    // 本身这些值就是动态的
    innerWinow = Object.defineProperty({}, "innerWidth", {
        value: window.innerWidth >> 0, // 2048, 会判断是否是800
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "innerHeight", {
        value: window.innerHeight >> 0, // 960, 会判断是否是600
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "outerWidth", {
        value: window.outerWidth >> 0, // 2554
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "outerHeight", {
        value: window.outerHeight >> 0, // 1386
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "availWidth", {
        value: window.screen.availWidth >> 0, // 2560
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "availHeight", {
        value: window.screen.availHeight >> 0, // 1392
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "sizeWidth", {
        value: ((window.screen.width >> 0) == 0) ? 2560 : (window.screen.sizeWidth >> 0), // 2560
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "sizeHeight", {
        value: ((window.screen.height >> 0) == 0) ? 1440 : (window.screen.sizeHeight >> 0), // 1440
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    innerWinow = Object.defineProperty(innerWinow, "platform", {
        value: navigator.platform, // Win32
        writable: !0,
        configurable: !0,
        enumerable: !0
    });

    U[75] = innerWinow;

    U[76] = "";
    keys = Object.keys.apply(Object, [U[75]]);
    for (var key in keys, innerWinow) {
        U[76] += innerWinow[key] + "|";
    }
    U[76] = U[76].substring(0, U[76].length - 1);

    U[77] = [];
    for (var i = 0; i < U[76].length; i++) {
        U[77].push(U[76].charCodeAt(i));
    }

    U[78] = U[77].length;
    U[79] = U[78] & 255;
    U[80] = (U[78] >> 8) & 255;
    U[81] = ((U[14] + 3) & 255) + ",";

    U[82] = [];
    for (var i = 0; i < U[81].length; i++) {
        U[82].push(U[81].charCodeAt(i));
    }

    U[83] = U[82].length;
    U[84] = U[83] & 255;
    U[85] = (U[83] >> 8) & 255;

    // 模拟检测，可不执行, performance
    // 有时候的检测和performance内容不尽相同
    var inObj1 = {0:[U[25][0], U[25][1]], length:1};
    var inObj2 = {0:[U[25][0], U[25][1]], 1:2, length:2};
    U[86] = random(inObj1).concat(random(inObj2));

    // 类似一个hash
    // 这里无 37（event), 50, 54, 58三项是写死的
    // 75， 76 window相关， 77 在后面， 78是77长度
    // 81是时间差串，82后有，83是82长度
    U[87] = U[86][0] ^ U[86][1] ^ U[86][2] ^ U[86][3] ^ U[86][4] ^ U[86][5] ^ U[86][6] ^ U[86][7] ^
        U[24] ^ U[26] ^ U[27] ^ U[28] ^ U[29] ^ U[30] ^ U[31] ^ U[32] ^ U[33] ^ U[34] ^
        U[35] ^ U[36] ^ U[38] ^ U[39] ^ U[40] ^ U[41] ^ U[42] ^ U[43] ^ U[44] ^ U[45] ^
        U[46] ^ U[47] ^ U[48] ^ U[49] ^ U[51] ^ U[52] ^ U[53] ^ U[55] ^ U[56] ^ U[57] ^
        U[59] ^ U[60] ^ U[61] ^ U[62] ^ U[63] ^ U[64] ^ U[65] ^ U[66] ^ U[67] ^ U[68] ^
        U[69] ^ U[70] ^ U[71] ^ U[72] ^ U[73] ^ U[74] ^ U[79] ^ U[80] ^ U[84] ^ U[85];


    // 摘自别人对我视频的评论，这个排序对应下面tlist先后顺序
    // sort_index = [
    //     0x09, 0x12, 0x1c, 0x20, 0x2c, 0x04, 0x29, 0x13, 0x0a, 0x17,
    //     0x0c, 0x25, 0x18, 0x27, 0x03, 0x16, 0x23, 0x15, 0x05, 0x2a,
    //     0x01, 0x1b, 0x06, 0x28, 0x1e, 0x0e, 0x21, 0x22, 0x02, 0x28,
    //     0x0f, 0x2d, 0x1d, 0x19, 0x10, 0x0d, 0x08, 0x26, 0x1a, 0x11,
    //     0x24, 0x14, 0x0b, 0x00, 0x1f, 0x07, 0x2e, 0x2f, 0x30, 0x31
    // ]
    tlist = [U[34], U[44], U[56], U[61], U[73], U[29], U[70], U[45], U[35], U[49],
    U[38], U[66], U[51], U[68], U[28], U[48], U[64], U[47], U[30], U[71],
    U[26], U[55], U[31], U[69], U[59], U[40], U[62], U[63], U[27], U[72],
    U[41], U[74], U[57], U[52], U[42], U[39], U[33], U[67], U[53], U[43],
    U[65], U[46], U[36], U[24], U[60], U[32], U[79], U[80], U[84], U[85]];

    // U88的生成涉及到是否是数组的判断, 是否是null，undefined
    // 如果是数组则拷贝的新数组，否则直接拿过来
    // 但是最终执行的还是concat，下面代码这么写，不影响逻辑
    // 因此注释的代码不需要了
    // u77 = U[77];
    // if(Array.isArray(Array, u77)) {
    //      newU77 = Array.apply(undefined, [U[77].length]);
    //      for(var i = 0; i < U[77].length]; i++) {
    //      newU77[i] = U[77][i];
    //      } 
    // } // 以此类推，略
    // U88的长度不是固定的，可能大于92，我测试和调试发现出现过92, 96, 98, 99
    U[88] = tlist.concat(U[77], U[82], [U[87]]);

    // 有人评论我，这个U89是4字节随机头部
    inObj = {0:U[23], 1:1, length:2};
    U[89] = random(inObj);
    // 调试运行时代码为String.fromCharCode.apply.apply(String.fromCharCode, [String, U[89]]);
    // 等价于下面代码，下面代码更简洁
    // 也等价于String.fromCharCode.apply(null, U[89]);
    // String.fromCharCode(num1, num2, /* …, */ numN)
    u89 = String.fromCharCode.apply(String, U[89]);
    U[89] = u89;

    U[90] = u88Transform(U[88]);

    // String.fromCharCode(num1, num2, /* …, */ numN)
    // 调试运行时代码为String.fromCharCode.apply.apply(String.fromCharCode, [null, [211]]);
    // 等价于下面代码，下面代码更简洁，
    nkey = String.fromCharCode(211);

    // 网上评论U86是8字节随机字节
    t1list = U[86]; 
    t1list = t1list.concat(U[90]);

    // 下面是调试运行时代码String.fromCharCode.apply.apply(String.fromCharCode, [null, t1list]);
    // 等价于下面代码，下面代码更简洁
    // 也等价于String.fromCharCode.apply(null, t1list);
    ncode = String.fromCharCode.apply(String, t1list);
    if (programVersion === G_DEBUG) {
        console.log("ncode :: ", ncode);
    }

    cryptNcode = dyRc4(nkey, ncode);
    U[91] = cryptNcode;

    U[92] = U[89] + U[91];

    // U93即a_bogus
    U[93] = dyBase64(U[92], "s4", null);


    if (programVersion === G_DEBUG) {
        console.log("Magic Array :: ", U);
    }

    if (programVersion === G_DEBUG) {
        console.log("a_bogus length :: ", U[93].length);
        console.log("a_bogus :: ", U[93]);
    }

    return U[93];

}

// 验证test, t1list, u90, u92都需要修改
function test() {
    nkey = String.fromCharCode(211);

    t1list = [129, 81, 32, 69, 33, 0, 170, 85];

    u90 = [16, 76, 200, 11, 88, 0, 235, 132, 0, 66,
        41, 0, 156, 193, 11, 129, 68, 95, 12, 133,
        101, 214, 12, 2, 200, 239, 54, 194, 188, 125,
        12, 2, 125, 2, 63, 188, 188, 70, 24, 11,
        144, 2, 94, 4, 195, 2, 40, 16, 135, 147,
        177, 178, 128, 67, 32, 0, 16, 41, 193, 4,
        61, 110, 36, 1, 149, 64, 26, 32, 32, 118,
        48, 56, 108, 49, 58, 48, 49, 60, 22, 112,
        180, 119, 48, 53, 237, 49, 27, 48, 40, 116,
        116, 62, 178, 119, 26, 52, 176, 62, 61, 112,
        179, 57, 30, 49, 236, 48, 49, 54, 166, 48,
        84, 60, 177, 116, 48, 53, 177, 60, 115, 84,
        249, 108, 63, 99, 35, 48, 56, 50, 35, 108,
        5, 28];

    t1list = t1list.concat(u90);

    console.log("t1list :: ", t1list);

    // 下面是调试运行时代码String.fromCharCode.apply.apply(String.fromCharCode, [null, t1list]);
    // 等价于下面代码，下面代码更简洁
    // 也等价于String.fromCharCode.apply(null, t1list);
    ncode = String.fromCharCode.apply(String, t1list);
    console.log("ncode :: ", ncode);

    cryptNcode = dyRc4(nkey, ncode);
    U[91] = cryptNcode;

    U[92] = "#WP\u0017" + U[91];

    // U93即a_bogus
    U[93] = dyBase64(U[92], "s4", "=");

    console.log("a_bogus length :: ", U[93].length);
    console.log("a_bogus :: ", U[93]);
}



/* ---- 出口：计算 a_bogus（uri 取 getTxUri(url,'get')='dhzx'，ts=0 用当前 enterPageTs） ---- */
window.__getABogus = function (url, method) {
    try {
        var uri = getTxUri(url || '', method || 'get');
        return makeABogus(uri, 0);
    } catch (e) {
        return '';
    }
};
