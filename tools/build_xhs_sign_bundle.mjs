#!/usr/bin/env node
/**
 * 把 Spider_XHS 签名内核（tools/xhs_sign_src/）打成 Android WebView 可执行的
 * 单文件 bundle（app/src/main/assets/xhs_sign_bundle.js）。
 *
 * 用法：
 *   node tools/build_xhs_sign_bundle.mjs            # 生成 bundle
 *   node tools/build_xhs_sign_bundle.mjs --test     # 生成 + 与 Node 原版金标对照
 *
 * 上游更新时：替换 tools/xhs_sign_src/ 里的文件后重跑本脚本即可。
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.join(__dirname, 'xhs_sign_src');
const OUT = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'xhs_sign_bundle.js');

const MODULES = [
  'mns.js', 'xs_common.js', 'sign.js', 'b1.js',
  'deflate.js', 'aes_encrypt.js', 'aes_decrypt.js', 'rap_crypto.js', 'rap.js',
];
const JSONS = ['mns_keystreams.json', 'mns_0101_keystream.json', 'rap_fingerprint_template.json'];

function transform(src, id) {
  // 相对 require → 注册表 id；内建 require 原样保留（由垫片解析）
  return src.replace(/require\(\s*['"]([^'"]+)['"]\s*\)/g, (m, p1) => {
    if (p1.startsWith('.')) return `require('${path.posix.basename(p1)}')`;
    return `require('${p1}')`;
  });
}

function build() {
  const parts = [];
  parts.push(`/* eslint-disable */
/*
 * xhs_sign_bundle.js — 由 tools/build_xhs_sign_bundle.mjs 生成，勿手改。
 * 内核来源：github.com/cv-cat/Spider_XHS（xhs_core/js + xhs_pc/js）。
 * 运行环境：Android WebView（V8）。Node 内建能力以垫片形式内联。
 */
(function (global) {
'use strict';
var __exports = {};
function __makeRequire() {
  var req = function (name) {
    if (name === 'crypto') return __shim.crypto;
    if (name === 'zlib') return __shim.zlib;
    if (name === 'fs') return __shim.fs;
    if (name === 'path') return __shim.path;
    if (name === 'vm') return __shim.vm;
    if (__modules[name]) return __modules[name].exports;
    throw new Error('xhs_sign_bundle: unknown module ' + name);
  };
  req.main = undefined;
  return req;
}
var __modules = {};
function __define(id, factory) {
  var module = { exports: {} };
  __modules[id] = module;
  factory(module, module.exports, __makeRequire(), id, '');
}
`);

  // ---- JSON 内联 ----
  parts.push('var __jsonFiles = {');
  for (const j of JSONS) {
    const raw = fs.readFileSync(path.join(SRC, j), 'utf8');
    parts.push(`  '${j}': ${JSON.stringify(raw)},`);
  }
  parts.push('};');

  // ---- 垫片 ----
  parts.push(`
var __shim = {};
// ---------- Buffer ----------
function __b64decode(s) {
  s = String(s).replace(/[^A-Za-z0-9+/=]/g, '');
  var tbl = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  var out = [], bits = 0, val = 0;
  for (var i = 0; i < s.length; i++) {
    var c = s.charAt(i);
    if (c === '=') break;
    var idx = tbl.indexOf(c);
    if (idx < 0) continue;
    val = (val << 6) | idx; bits += 6;
    if (bits >= 8) { bits -= 8; out.push((val >> bits) & 0xff); }
  }
  return out;
}
function __b64encode(bytes) {
  var tbl = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  var out = '';
  for (var i = 0; i < bytes.length; i += 3) {
    var b0 = bytes[i], b1 = bytes[i + 1], b2 = bytes[i + 2];
    out += tbl[b0 >> 2];
    out += tbl[((b0 & 3) << 4) | ((b1 === undefined ? 0 : b1) >> 4)];
    out += b1 === undefined ? '=' : tbl[((b1 & 15) << 2) | ((b2 === undefined ? 0 : b2) >> 6)];
    out += b2 === undefined ? '=' : tbl[b2 & 63];
  }
  return out;
}
function __utf8bytes(s) {
  var out = [], enc = encodeURIComponent(String(s));
  for (var i = 0; i < enc.length; i++) {
    if (enc.charAt(i) === '%') { out.push(parseInt(enc.substr(i + 1, 2), 16)); i += 2; }
    else out.push(enc.charCodeAt(i));
  }
  return out;
}
function __hexdecode(s) {
  s = String(s).replace(/[^0-9a-fA-F]/g, '');
  var out = [];
  for (var i = 0; i + 1 < s.length; i += 2) out.push(parseInt(s.substr(i, 2), 16));
  return out;
}
function __utf8decode(bytes) {
  var out = '', k = 0;
  while (k < bytes.length) {
    var c = bytes[k++];
    if (c < 0x80) out += String.fromCharCode(c);
    else if (c < 0xe0) out += String.fromCharCode(((c & 0x1f) << 6) | (bytes[k++] & 0x3f));
    else if (c < 0xf0) out += String.fromCharCode(((c & 0x0f) << 12) | ((bytes[k++] & 0x3f) << 6) | (bytes[k++] & 0x3f));
    else {
      var cp = ((c & 0x07) << 18) | ((bytes[k++] & 0x3f) << 12) | ((bytes[k++] & 0x3f) << 6) | (bytes[k++] & 0x3f);
      cp -= 0x10000;
      out += String.fromCharCode(0xd800 + (cp >> 10), 0xdc00 + (cp & 0x3ff));
    }
  }
  return out;
}
function BufferShim(arg, enc) {
  var bytes;
  if (typeof arg === 'number') { bytes = new Uint8Array(arg); }
  else if (arg instanceof Uint8Array) { bytes = new Uint8Array(arg); }
  else if (Array.isArray(arg)) { bytes = Uint8Array.from(arg); }
  else if (typeof arg === 'string') {
    if (enc === 'base64') bytes = Uint8Array.from(__b64decode(arg));
    else if (enc === 'hex') bytes = Uint8Array.from(__hexdecode(arg));
    else bytes = Uint8Array.from(__utf8bytes(arg));
  } else { bytes = new Uint8Array(0); }
  var buf = new Uint8Array(bytes);
  Object.setPrototypeOf(buf, BufferShim.prototype);
  return buf;
}
BufferShim.prototype = Object.create(Uint8Array.prototype);
BufferShim.prototype.constructor = BufferShim;
BufferShim.prototype.readUInt32LE = function (off) {
  return (this[off] | (this[off + 1] << 8) | (this[off + 2] << 16) | (this[off + 3] << 24)) >>> 0;
};
BufferShim.prototype.writeUInt32LE = function (v, off) {
  this[off] = v & 0xff; this[off + 1] = (v >>> 8) & 0xff;
  this[off + 2] = (v >>> 16) & 0xff; this[off + 3] = (v >>> 24) & 0xff;
  return off + 4;
};
BufferShim.prototype.readUInt32BE = function (off) {
  return ((this[off] << 24) | (this[off + 1] << 16) | (this[off + 2] << 8) | this[off + 3]) >>> 0;
};
BufferShim.prototype.writeUInt32BE = function (v, off) {
  this[off] = (v >>> 24) & 0xff; this[off + 1] = (v >>> 16) & 0xff;
  this[off + 2] = (v >>> 8) & 0xff; this[off + 3] = v & 0xff;
  return off + 4;
};
BufferShim.prototype.copy = function (tgt, tOff) {
  tOff = tOff || 0;
  for (var i = 0; i < this.length; i++) tgt[tOff + i] = this[i];
  return this.length;
};
BufferShim.prototype.toString = function (enc) {
  if (enc === 'base64') return __b64encode(this);
  if (enc === 'hex') { var h = ''; for (var i = 0; i < this.length; i++) h += (this[i] < 16 ? '0' : '') + this[i].toString(16); return h; }
  return __utf8decode(this);
};
BufferShim.prototype.slice = function (a, b) { return BufferShim.prototype.subarray.call(this, a, b); };
BufferShim.from = function (arg, enc) { return BufferShim(arg, enc); };
BufferShim.alloc = function (n) { return BufferShim(n); };
BufferShim.allocUnsafe = function (n) { return BufferShim(n); };
BufferShim.concat = function (arr) {
  var total = 0, i;
  for (i = 0; i < arr.length; i++) total += arr[i].length;
  var out = BufferShim(total), off = 0;
  for (i = 0; i < arr.length; i++) { out.set(arr[i], off); off += arr[i].length; }
  return out;
};
BufferShim.byteLength = function (s, enc) {
  return (enc === 'utf8' || enc === undefined) ? __utf8bytes(s).length : String(s).length;
};
var __Buffer = BufferShim;

// ---------- md5（blueimp/JavaScript-MD5，MIT）/ sha1 ----------
var __md5fn = (function () {
  var module = { exports: {} };
  (function (module) {
/*
 * JavaScript MD5
 * https://github.com/blueimp/JavaScript-MD5
 *
 * Copyright 2011, Sebastian Tschan
 * https://blueimp.net
 *
 * Licensed under the MIT license:
 * https://opensource.org/licenses/MIT
 *
 * Based on
 * A JavaScript implementation of the RSA Data Security, Inc. MD5 Message
 * Digest Algorithm, as defined in RFC 1321.
 * Version 2.2 Copyright (C) Paul Johnston 1999 - 2009
 * Other contributors: Greg Holt, Andrew Kepert, Ydnar, Lostinet
 * Distributed under the BSD License
 * See http://pajhome.org.uk/crypt/md5 for more info.
 */

/* global define */

/* eslint-disable strict */

;(function ($) {
  'use strict'

  /**
   * Add integers, wrapping at 2^32.
   * This uses 16-bit operations internally to work around bugs in interpreters.
   *
   * @param {number} x First integer
   * @param {number} y Second integer
   * @returns {number} Sum
   */
  function safeAdd(x, y) {
    var lsw = (x & 0xffff) + (y & 0xffff)
    var msw = (x >> 16) + (y >> 16) + (lsw >> 16)
    return (msw << 16) | (lsw & 0xffff)
  }

  /**
   * Bitwise rotate a 32-bit number to the left.
   *
   * @param {number} num 32-bit number
   * @param {number} cnt Rotation count
   * @returns {number} Rotated number
   */
  function bitRotateLeft(num, cnt) {
    return (num << cnt) | (num >>> (32 - cnt))
  }

  /**
   * Basic operation the algorithm uses.
   *
   * @param {number} q q
   * @param {number} a a
   * @param {number} b b
   * @param {number} x x
   * @param {number} s s
   * @param {number} t t
   * @returns {number} Result
   */
  function md5cmn(q, a, b, x, s, t) {
    return safeAdd(bitRotateLeft(safeAdd(safeAdd(a, q), safeAdd(x, t)), s), b)
  }
  /**
   * Basic operation the algorithm uses.
   *
   * @param {number} a a
   * @param {number} b b
   * @param {number} c c
   * @param {number} d d
   * @param {number} x x
   * @param {number} s s
   * @param {number} t t
   * @returns {number} Result
   */
  function md5ff(a, b, c, d, x, s, t) {
    return md5cmn((b & c) | (~b & d), a, b, x, s, t)
  }
  /**
   * Basic operation the algorithm uses.
   *
   * @param {number} a a
   * @param {number} b b
   * @param {number} c c
   * @param {number} d d
   * @param {number} x x
   * @param {number} s s
   * @param {number} t t
   * @returns {number} Result
   */
  function md5gg(a, b, c, d, x, s, t) {
    return md5cmn((b & d) | (c & ~d), a, b, x, s, t)
  }
  /**
   * Basic operation the algorithm uses.
   *
   * @param {number} a a
   * @param {number} b b
   * @param {number} c c
   * @param {number} d d
   * @param {number} x x
   * @param {number} s s
   * @param {number} t t
   * @returns {number} Result
   */
  function md5hh(a, b, c, d, x, s, t) {
    return md5cmn(b ^ c ^ d, a, b, x, s, t)
  }
  /**
   * Basic operation the algorithm uses.
   *
   * @param {number} a a
   * @param {number} b b
   * @param {number} c c
   * @param {number} d d
   * @param {number} x x
   * @param {number} s s
   * @param {number} t t
   * @returns {number} Result
   */
  function md5ii(a, b, c, d, x, s, t) {
    return md5cmn(c ^ (b | ~d), a, b, x, s, t)
  }

  /**
   * Calculate the MD5 of an array of little-endian words, and a bit length.
   *
   * @param {Array} x Array of little-endian words
   * @param {number} len Bit length
   * @returns {Array<number>} MD5 Array
   */
  function binlMD5(x, len) {
    /* append padding */
    x[len >> 5] |= 0x80 << len % 32
    x[(((len + 64) >>> 9) << 4) + 14] = len

    var i
    var olda
    var oldb
    var oldc
    var oldd
    var a = 1732584193
    var b = -271733879
    var c = -1732584194
    var d = 271733878

    for (i = 0; i < x.length; i += 16) {
      olda = a
      oldb = b
      oldc = c
      oldd = d

      a = md5ff(a, b, c, d, x[i], 7, -680876936)
      d = md5ff(d, a, b, c, x[i + 1], 12, -389564586)
      c = md5ff(c, d, a, b, x[i + 2], 17, 606105819)
      b = md5ff(b, c, d, a, x[i + 3], 22, -1044525330)
      a = md5ff(a, b, c, d, x[i + 4], 7, -176418897)
      d = md5ff(d, a, b, c, x[i + 5], 12, 1200080426)
      c = md5ff(c, d, a, b, x[i + 6], 17, -1473231341)
      b = md5ff(b, c, d, a, x[i + 7], 22, -45705983)
      a = md5ff(a, b, c, d, x[i + 8], 7, 1770035416)
      d = md5ff(d, a, b, c, x[i + 9], 12, -1958414417)
      c = md5ff(c, d, a, b, x[i + 10], 17, -42063)
      b = md5ff(b, c, d, a, x[i + 11], 22, -1990404162)
      a = md5ff(a, b, c, d, x[i + 12], 7, 1804603682)
      d = md5ff(d, a, b, c, x[i + 13], 12, -40341101)
      c = md5ff(c, d, a, b, x[i + 14], 17, -1502002290)
      b = md5ff(b, c, d, a, x[i + 15], 22, 1236535329)

      a = md5gg(a, b, c, d, x[i + 1], 5, -165796510)
      d = md5gg(d, a, b, c, x[i + 6], 9, -1069501632)
      c = md5gg(c, d, a, b, x[i + 11], 14, 643717713)
      b = md5gg(b, c, d, a, x[i], 20, -373897302)
      a = md5gg(a, b, c, d, x[i + 5], 5, -701558691)
      d = md5gg(d, a, b, c, x[i + 10], 9, 38016083)
      c = md5gg(c, d, a, b, x[i + 15], 14, -660478335)
      b = md5gg(b, c, d, a, x[i + 4], 20, -405537848)
      a = md5gg(a, b, c, d, x[i + 9], 5, 568446438)
      d = md5gg(d, a, b, c, x[i + 14], 9, -1019803690)
      c = md5gg(c, d, a, b, x[i + 3], 14, -187363961)
      b = md5gg(b, c, d, a, x[i + 8], 20, 1163531501)
      a = md5gg(a, b, c, d, x[i + 13], 5, -1444681467)
      d = md5gg(d, a, b, c, x[i + 2], 9, -51403784)
      c = md5gg(c, d, a, b, x[i + 7], 14, 1735328473)
      b = md5gg(b, c, d, a, x[i + 12], 20, -1926607734)

      a = md5hh(a, b, c, d, x[i + 5], 4, -378558)
      d = md5hh(d, a, b, c, x[i + 8], 11, -2022574463)
      c = md5hh(c, d, a, b, x[i + 11], 16, 1839030562)
      b = md5hh(b, c, d, a, x[i + 14], 23, -35309556)
      a = md5hh(a, b, c, d, x[i + 1], 4, -1530992060)
      d = md5hh(d, a, b, c, x[i + 4], 11, 1272893353)
      c = md5hh(c, d, a, b, x[i + 7], 16, -155497632)
      b = md5hh(b, c, d, a, x[i + 10], 23, -1094730640)
      a = md5hh(a, b, c, d, x[i + 13], 4, 681279174)
      d = md5hh(d, a, b, c, x[i], 11, -358537222)
      c = md5hh(c, d, a, b, x[i + 3], 16, -722521979)
      b = md5hh(b, c, d, a, x[i + 6], 23, 76029189)
      a = md5hh(a, b, c, d, x[i + 9], 4, -640364487)
      d = md5hh(d, a, b, c, x[i + 12], 11, -421815835)
      c = md5hh(c, d, a, b, x[i + 15], 16, 530742520)
      b = md5hh(b, c, d, a, x[i + 2], 23, -995338651)

      a = md5ii(a, b, c, d, x[i], 6, -198630844)
      d = md5ii(d, a, b, c, x[i + 7], 10, 1126891415)
      c = md5ii(c, d, a, b, x[i + 14], 15, -1416354905)
      b = md5ii(b, c, d, a, x[i + 5], 21, -57434055)
      a = md5ii(a, b, c, d, x[i + 12], 6, 1700485571)
      d = md5ii(d, a, b, c, x[i + 3], 10, -1894986606)
      c = md5ii(c, d, a, b, x[i + 10], 15, -1051523)
      b = md5ii(b, c, d, a, x[i + 1], 21, -2054922799)
      a = md5ii(a, b, c, d, x[i + 8], 6, 1873313359)
      d = md5ii(d, a, b, c, x[i + 15], 10, -30611744)
      c = md5ii(c, d, a, b, x[i + 6], 15, -1560198380)
      b = md5ii(b, c, d, a, x[i + 13], 21, 1309151649)
      a = md5ii(a, b, c, d, x[i + 4], 6, -145523070)
      d = md5ii(d, a, b, c, x[i + 11], 10, -1120210379)
      c = md5ii(c, d, a, b, x[i + 2], 15, 718787259)
      b = md5ii(b, c, d, a, x[i + 9], 21, -343485551)

      a = safeAdd(a, olda)
      b = safeAdd(b, oldb)
      c = safeAdd(c, oldc)
      d = safeAdd(d, oldd)
    }
    return [a, b, c, d]
  }

  /**
   * Convert an array of little-endian words to a string
   *
   * @param {Array<number>} input MD5 Array
   * @returns {string} MD5 string
   */
  function binl2rstr(input) {
    var i
    var output = ''
    var length32 = input.length * 32
    for (i = 0; i < length32; i += 8) {
      output += String.fromCharCode((input[i >> 5] >>> i % 32) & 0xff)
    }
    return output
  }

  /**
   * Convert a raw string to an array of little-endian words
   * Characters >255 have their high-byte silently ignored.
   *
   * @param {string} input Raw input string
   * @returns {Array<number>} Array of little-endian words
   */
  function rstr2binl(input) {
    var i
    var output = []
    output[(input.length >> 2) - 1] = undefined
    for (i = 0; i < output.length; i += 1) {
      output[i] = 0
    }
    var length8 = input.length * 8
    for (i = 0; i < length8; i += 8) {
      output[i >> 5] |= (input.charCodeAt(i / 8) & 0xff) << i % 32
    }
    return output
  }

  /**
   * Calculate the MD5 of a raw string
   *
   * @param {string} s Input string
   * @returns {string} Raw MD5 string
   */
  function rstrMD5(s) {
    return binl2rstr(binlMD5(rstr2binl(s), s.length * 8))
  }

  /**
   * Calculates the HMAC-MD5 of a key and some data (raw strings)
   *
   * @param {string} key HMAC key
   * @param {string} data Raw input string
   * @returns {string} Raw MD5 string
   */
  function rstrHMACMD5(key, data) {
    var i
    var bkey = rstr2binl(key)
    var ipad = []
    var opad = []
    var hash
    ipad[15] = opad[15] = undefined
    if (bkey.length > 16) {
      bkey = binlMD5(bkey, key.length * 8)
    }
    for (i = 0; i < 16; i += 1) {
      ipad[i] = bkey[i] ^ 0x36363636
      opad[i] = bkey[i] ^ 0x5c5c5c5c
    }
    hash = binlMD5(ipad.concat(rstr2binl(data)), 512 + data.length * 8)
    return binl2rstr(binlMD5(opad.concat(hash), 512 + 128))
  }

  /**
   * Convert a raw string to a hex string
   *
   * @param {string} input Raw input string
   * @returns {string} Hex encoded string
   */
  function rstr2hex(input) {
    var hexTab = '0123456789abcdef'
    var output = ''
    var x
    var i
    for (i = 0; i < input.length; i += 1) {
      x = input.charCodeAt(i)
      output += hexTab.charAt((x >>> 4) & 0x0f) + hexTab.charAt(x & 0x0f)
    }
    return output
  }

  /**
   * Encode a string as UTF-8
   *
   * @param {string} input Input string
   * @returns {string} UTF8 string
   */
  function str2rstrUTF8(input) {
    return unescape(encodeURIComponent(input))
  }

  /**
   * Encodes input string as raw MD5 string
   *
   * @param {string} s Input string
   * @returns {string} Raw MD5 string
   */
  function rawMD5(s) {
    return rstrMD5(str2rstrUTF8(s))
  }
  /**
   * Encodes input string as Hex encoded string
   *
   * @param {string} s Input string
   * @returns {string} Hex encoded string
   */
  function hexMD5(s) {
    return rstr2hex(rawMD5(s))
  }
  /**
   * Calculates the raw HMAC-MD5 for the given key and data
   *
   * @param {string} k HMAC key
   * @param {string} d Input string
   * @returns {string} Raw MD5 string
   */
  function rawHMACMD5(k, d) {
    return rstrHMACMD5(str2rstrUTF8(k), str2rstrUTF8(d))
  }
  /**
   * Calculates the Hex encoded HMAC-MD5 for the given key and data
   *
   * @param {string} k HMAC key
   * @param {string} d Input string
   * @returns {string} Raw MD5 string
   */
  function hexHMACMD5(k, d) {
    return rstr2hex(rawHMACMD5(k, d))
  }

  /**
   * Calculates MD5 value for a given string.
   * If a key is provided, calculates the HMAC-MD5 value.
   * Returns a Hex encoded string unless the raw argument is given.
   *
   * @param {string} string Input string
   * @param {string} [key] HMAC key
   * @param {boolean} [raw] Raw output switch
   * @returns {string} MD5 output
   */
  function md5(string, key, raw) {
    if (!key) {
      if (!raw) {
        return hexMD5(string)
      }
      return rawMD5(string)
    }
    if (!raw) {
      return hexHMACMD5(key, string)
    }
    return rawHMACMD5(key, string)
  }

  if (typeof define === 'function' && define.amd) {
    define(function () {
      return md5
    })
  } else if (typeof module === 'object' && module.exports) {
    module.exports = md5
  } else {
    $.md5 = md5
  }
})(this)

  })(module);
  return module.exports;
})();
function __sha1bytes(bytes) {
  throw new Error('xhs_sign_bundle: sha1 (webSsk) not supported');
}

// ---------- crypto ----------
__shim.crypto = {
  randomBytes: function (n) {
    var out = new Uint8Array(n);
    if (global.crypto && global.crypto.getRandomValues) global.crypto.getRandomValues(out);
    else for (var i = 0; i < n; i++) out[i] = Math.floor(Math.random() * 256);
    return __Buffer.from(out);
  },
  createHash: function (alg) {
    if (alg !== 'md5') return { update: function () { return this; }, digest: function () { return __sha1bytes([]); } };
    var parts = [];
    return {
      update: function (data, enc) {
        if (typeof data !== 'string') throw new Error('xhs_sign_bundle: md5 update only accepts strings');
        parts.push(data);
        return this;
      },
      digest: function (enc) {
        var hex = __md5fn(parts.join(''));
        if (enc === 'hex') return hex;
        var out = [];
        for (var i = 0; i < hex.length; i += 2) out.push(parseInt(hex.substr(i, 2), 16));
        return __Buffer.from(out);
      },
    };
  },
};

// ---------- zlib ----------
function __crc32(bytes) {
  var table = __crc32.t;
  if (!table) {
    table = __crc32.t = [];
    for (var n = 0; n < 256; n++) {
      var c = n;
      for (var k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      table[n] = c >>> 0;
    }
  }
  var crc = 0xffffffff;
  for (var i = 0; i < bytes.length; i++) crc = (crc >>> 8) ^ table[(crc ^ bytes[i]) & 0xff];
  return (crc ^ 0xffffffff) >>> 0;
}
__shim.zlib = {
  crc32: function (buf) { return __crc32(buf instanceof Uint8Array ? buf : __Buffer.from(buf)); },
  deflateRawSync: function () { throw new Error('xhs_sign_bundle: zlib.deflateRawSync unavailable (encodeGzip fallback path)'); },
  gunzipSync: function () { throw new Error('xhs_sign_bundle: zlib.gunzipSync unavailable (decrypt verify path)'); },
};

// ---------- fs / path / vm / process ----------
__shim.fs = {
  readFileSync: function (p) {
    var base = String(p).split(/[\\\\/]/).pop();
    if (__jsonFiles[base] !== undefined) return __jsonFiles[base];
    throw new Error('xhs_sign_bundle: fs.readFileSync miss ' + p);
  },
};
__shim.path = {
  join: function () { return Array.prototype.slice.call(arguments).join('/'); },
  dirname: function (p) { return String(p).split(/[\\\\/]/).slice(0, -1).join('/'); },
  basename: function (p) { return String(p).split(/[\\\\/]/).pop(); },
};
__shim.vm = {
  runInNewContext: function (source, sandbox, opts) {
    var fn = new Function('sandbox', 'with (sandbox) { return (function () { ' + source + '\\n; })(); }');
    return fn(sandbox || {});
  },
};
__shim.process = { argv: [], stdout: { write: function () {} }, exit: function () {}, env: {} };
global.Buffer = __Buffer;
// 影子 Buffer：模块代码里的裸 Buffer 必须解析到 shim（WebView 无 Node Buffer），
// 否则金标测试会漏到 Node 真 Buffer 上，掩盖垫片缺口。
var Buffer = __Buffer;
`);

  // ---- 模块定义 ----
  for (const id of MODULES) {
    const raw = fs.readFileSync(path.join(SRC, id), 'utf8');
    parts.push(`__define(${JSON.stringify(id)}, function (module, exports, require, __filename, __dirname) {`);
    parts.push(transform(raw, id));
    parts.push('});');
  }

  // ---- 公共 API ----
  parts.push(`
var __sign = __modules['sign.js'].exports;
var __b1 = __modules['b1.js'].exports;
var __rap = __modules['rap.js'].exports;
__exports.signFull = function (inputJson) {
  return JSON.stringify(__sign.signFull(JSON.parse(inputJson)));
};
__exports.generateB1 = function (optionsJson) {
  return String(__b1.generateB1(JSON.parse(optionsJson)));
};
__exports.buildRap = function (api, dataJson, fingerprintHex, optsJson) {
  var fpHex = fingerprintHex || '';
  if (!fpHex) {
    var tpl = JSON.parse(__shim.fs.readFileSync('rap_fingerprint_template.json', 'utf8'));
    fpHex = tpl.bodyUnmaskedHex || tpl.bodyUnmappedHex;
  }
  var args = { api: api, data: dataJson || '', fingerprint: __Buffer.from(fpHex, 'hex') };
  if (optsJson) {
    var o = JSON.parse(optsJson);
    for (var k in o) {
      if (Array.isArray(o[k])) args[k] = __Buffer.from(o[k]);
      else args[k] = o[k];
    }
  }
  return String(__rap.buildRapPure(args));
};
global.XhsSign = __exports;
})(typeof globalThis !== 'undefined' ? globalThis : this);
`);
  const out = parts.join('\n');
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, out);
  console.log('bundle written:', OUT, out.length, 'bytes');
}

function test() {
  const require_ = createRequire(import.meta.url);
  const origSign = require_(path.join(SRC, 'sign.js'));
  const origRap = require_(path.join(SRC, 'rap.js'));
  const bundleSrc = fs.readFileSync(OUT, 'utf8');
  const sandboxGlobal = { crypto: { getRandomValues: (a) => a.fill(7) } };
  new Function('globalThis', bundleSrc)(sandboxGlobal);
  const XhsSign = sandboxGlobal.XhsSign;

  // 1) signFull：固定输入 → xs/xt/x3 必须逐字节一致
  const input = {
    api: '/api/sns/web/v1/user_posted?num=30&cursor=&user_id=64b411bb000000001001e569&image_formats=jpg,webp,avif',
    data: '',
    cookie: 'a1=test1234567890abcdef; webId=5fabcdef0123456789abcdef01',
    a1: 'test1234567890abcdef',
    b1: '',
    dslPair: 'null;1234567890',
    tier: '0101',
    now: 1792660000000,
    xt: '1792660000000',
    version: 123456789,
    loadts: 1792660000000,
    seq: 3,
    envConst: 1731743427,
    envFpTail: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14],
    webBuild: '6.47.2',
    signVersion: '4.4.3',
    appId: 'xhs-pc-web',
    platform: 'Windows',
    deviceTag: '',
  };
  const o = origSign.signFull(input);
  const b = JSON.parse(XhsSign.signFull(JSON.stringify(input)));
  const keys = ['xs', 'xt', 'x3', 'xs_common'];
  let pass = 0, fail = 0;
  for (const k of keys) {
    const same = String(o[k] || '') === String(b[k] || '');
    if (same) pass++; else { fail++; console.log(`  MISMATCH ${k}:\n    orig=${String(o[k] || '').slice(0, 60)}\n    bundle=${String(b[k] || '').slice(0, 60)}`); }
  }
  console.log(`signFull: ${pass} same, ${fail} mismatch`);

  // 2) buildRap：固定随机源 → 一致
  const { Buffer: NodeBuffer } = require_('node:buffer');
  const rapArgs = {
    api: '/api/sns/web/v1/user_posted?num=30',
    data: '',
    fingerprint: NodeBuffer.from(
      JSON.parse(fs.readFileSync(path.join(SRC, 'rap_fingerprint_template.json'), 'utf8')).bodyUnmaskedHex, 'hex'),
    mask: 120, xorKey: NodeBuffer.from([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]),
    ts: 1792660000000, nonce: NodeBuffer.from([97, 98, 99, 100]), mtime: 0,
    padBytes: NodeBuffer.from([48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102]),
    bodyEncryTime: 1,
  };
  const ro = String(origRap.buildRapPure(rapArgs));
  const opts = JSON.stringify({
    mask: 120, xorKey: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16],
    ts: 1792660000000, nonce: [97, 98, 99, 100], mtime: 0,
    padBytes: [48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102],
    bodyEncryTime: 1,
  });
  const rb = XhsSign.buildRap(rapArgs.api, rapArgs.data, '', opts);
  console.log(`buildRap: orig len=${ro.length} / bundle len=${rb.length}`);
  if (ro === rb) { pass++; console.log('  buildRap byte-identical'); }
  else { fail++; console.log(`  MISMATCH rap://n    orig=${ro}\n    bundle=${rb}`); }

  console.log(`\n${pass} pass, ${fail} fail`);
  process.exit(fail ? 1 : 0);
}

build();
if (process.argv.includes('--test')) test();
