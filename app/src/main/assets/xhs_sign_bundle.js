/* eslint-disable */
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

var __jsonFiles = {
  'mns_keystreams.json': "{\"len\":2048,\"b64\":\"caMCJXeTJx3dJzvO4+S5jZ15NeHaM/V2Xi6or7bcd6UaSZ0jtnwgZgAlhgy/E9RUDZJJf1hobFdOUI9G4ZVjRPOROb9PryKj7vEgt5JYFFsv61GTtkeGaZYSmOeb7cpkbhppOpJhVKWnob0c8N7bdC+RenR6HjiLI08id1FttxFgNUOXMPph6YIqDsp7/3LYuiyDzNAm7K7b0M8PE+qxGcftcdHkES1hGoTMeqIO5IOK9bAdyZ9Ua86pkQYMdu2cKikkJSCBj6/Gzd4zjce19s6yvX8DPj5xI+vQA4B9YerOnYOfKXwyxXk1vR+pZRck5g17NB/umq5WUdT0brc32dQ/vEOK2WPs9nmN3N/LE/QkhvDsBMr4U0i6TFjZqPMkk2qFXj2QiMNKclFozf+EwF4S8GfypYc071HDNOzn+6E7Pebc3oX2tQP/BIpRh0ctyxunjJ0XLaNEmC8K7+Xq7QqIIPT/pcaPO/RxN1l9j/F5RP3wRBVoQq/+ckkhzRHX3pRsrkXYMeLm/YrGCVTgONUprhQTXiGHBt7lqXfJZe2sifN1NJ/Y7AFCHLyHpFaUOJynJQR2h7xFqgroroNR96qobgSX9wtzWKy7tuXJY/foUSMZrNKCd0DwsJdeNQQT/AxVKJV0S2kPyRYD8F0c806kIuOyjgFtiIMXt/l9zinKiUIeARq99gKS+A0zBusxfgzmGKRuU0xwUzN0pDBoC2t3KJHFxk3JDwNgnSIrKd+xKUwm2qYuTdnnSnH8B9L2NrFPSUqPaXmm1kB+kFJF6UVaWQTdfgcXCNTARqjCno4fIwaTQR2yUDGcLW3jwfSD5kR5EqvSuowYQV2Klr2boaCcbqfplJTahQmv4/kjCgsdGe/zzAe5FL8V5r6uq0ZOXt+hsOu+zGluc4BtvHRf+ml561z6QDjyHph9ebP46S3bG1xF7YGL97i7lGKdh+gA4AXDJRqg1TyA/35/n2PTjq2RGc27DQqwpW7Ig02h2enYqrG7vnf6/o/NGuwIqI6GKz97JgiZ27YE7h3/rGa0kDCQRDAW4K27445Z50eQUyMJ0giu0JDxmqxbiUBx1Fy3KvRNLn6Eff7REe7I269LB1nmsXRDaq6Nt2wK02RL0pVy+V1aIPCKRu7cvSxvd0ft4LkJ721oFY7rXFcdi0OTai8c6NR56Nvh8VU/Krt2cegnzXLozqVk4zyf40vVLdFfY0m+Gc2bAcSDnwqn9fAyCbBZXSAcxJpfo/YnSlSsJftRpzf5TyryQwhcG1fHRKJ1rwJLgzOKpKgFEMorDtklgBhKZzR7ZaE095T5VU1xD28kXQBBWStfJrMKlm1rIOhrP46sIcUwPrkoGo6UBVg7lSOue2kzAk5AAdeCWplT3kIsUnhwTBzLs7LEpER8IMEwjMH3cGTSgU6er6JS6TbXRnDPdxJ0B3ZAPLW5/dl2nRT1ukTmjML/EYN5KfcK0eknyXxJF7rbcWM9a4kYAri2BnSIW+aJzY3JBlQ4zvHfbDz6oQYNKcNgIb9EqG5/eZQf5zh3ZyjpXpYMLx6e281S+iS+NphCurDhwdgEwL1/JyxU7NQ25ub0hZiaJkSVabJPHurrz9bcyOCZAm1Ud8An9P+t9iVexHQ86zGrAXqWn9Ak432VmsPVOmuySesy9J6jyO/EVSrL/AKS4vF3d2XT1Vec70mKhjRSFY1H3IQCC+tyS5OF75/0ZcfQzL8xw4C6aAg8BbuQUwuyi3dY/7+kuApRO74Iv3XqSzDkpF/7puu0X3LccG4dfM3Y8uM2IQOrWuWLW+dNlNgKh55SdDmU55AFl9b+9c7Jl754V5F1/SCBJquZPN4PKigIF9nY8j+rey/JyGI4ADXuFjO/eggsUBMlqJpse2iC+IEJH80WLXL7KzXsinvml1cTHbfcL1Afkw74oEAxT3wjr2wqqblTc/Q/9JRCtVfhjMt4Orr/aTa8sCpnqdp3GSOoHxIKD4AVZbNAU1Qm2A27o8b5O2rObsLKVBB9EWl2XmxZfn9PNO3HE66dLJVYt5HuNyVF92YuxCPiyt+OphWvHEYFTRqgGdPRWGuYg4os3vodNtP6Ter7mse1LhbGVV+5jN83DSNyOCRqu7zR9ZxeGQuPUQRzE4rnEqszkKZ5/5oG4670QAgDpN8EYc28/8n2Az4d9okK7ucETRn9dqHQ7MRmG6OunBr701R2GZXUK4INLOOkkl4pU6SwmfmEn5E6jlNohGXha9i0TqCox78Sc2mIyr6rqVSW3kcwxkzovRFvFK9zNC7aq6fQ0aHWMtQMD5pLbqyqitPtT+1jYGLjJBik2cQTzQJGxnpFNyhlGf/YE6WcpNjO15FOXx1A35tBxkYj3smYUGABB8SSc2orIaPnz5OYnLdEbC7j91MyIksu3LZIswvrk7Jv0oiNICLuFMZtwEFcmomLpwU5Midf2ek3KfEnG8ymbCh4fczWyQfUtsGqOK2CMlDc2GzyRxauPxWD4SxJdFv+BuP+ykHOMchhSPldyYYJrePMyFtQm+qDzv3f+EZfsLt2IXbHK1JwUqKW4d+v7GyQ0ulJir/j9po87IALKkuwbnTWdSjoICtEKmf8Jo9ehKu40bWYPh5sIdpcsJd/6pD1Sa29qlqMVSTOc3O9LumtdFuGdBl/qbVxnzGcjGCDsl0/5A2ghjVrW2DwgBWj8eN9V7BkhCNzoMw8HRm3SnPehuAdbboPITim0nxdrG4=\"}",
  'mns_0101_keystream.json': "{\r\n  \"source\": \"browser-cold-start-fixtures-20260716\",\r\n  \"length\": 160,\r\n  \"b64\": \"r1crlcplstnsdrtdLpfLZTKZzGYzmcxmM5nM5nM5nM5nMxkMBgMBAAAAAACAQCCQSCQSicTicTgcDgcDAYBAoFAoFIrFYjEYDAaDwWAwmEwmk8lkslmsVqvV6vX6/X4/n08nk0mk0ul0Op1OJ5NJpNLp9Ho9Ho9HI5FIpNJpNBqNRiMRCIRCIZDIZDKZTKbT6XS63e53Ox2OR6NRKBSKxQ==\"\r\n}\r\n",
  'rap_fingerprint_template.json': "{\r\n  \"captured\": \"2026-07-14\",\r\n  \"source\": \"capture_20260714.json sample[1] (homefeed 235B)\",\r\n  \"mask\": \"0x38\",\r\n  \"plLen\": 235,\r\n  \"bodyLen\": 219,\r\n  \"bodyUnmaskedHex\": \"03ea00000010346c326b326a6a673668696d6472366203ebb0037bbb041b00041c00041d00041e00041f00042000042100042200042300042400042500042600042700042800042900042e00042a00042b00042c00042d00044c00000000042f00043000043100043600000000043a00000000043c00000000044000000000044200000000044900000012000000019f615912f2010000019f617b376d044400000a000446000004f704470000019f615908a50445000000000448000443000000040012ffff048400048500000000048000048100048200048300\"\r\n}",
};

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
    var base = String(p).split(/[\\/]/).pop();
    if (__jsonFiles[base] !== undefined) return __jsonFiles[base];
    throw new Error('xhs_sign_bundle: fs.readFileSync miss ' + p);
  },
};
__shim.path = {
  join: function () { return Array.prototype.slice.call(arguments).join('/'); },
  dirname: function (p) { return String(p).split(/[\\/]/).slice(0, -1).join('/'); },
  basename: function (p) { return String(p).split(/[\\/]/).pop(); },
};
__shim.vm = {
  runInNewContext: function (source, sandbox, opts) {
    var fn = new Function('sandbox', 'with (sandbox) { return (function () { ' + source + '\n; })(); }');
    return fn(sandbox || {});
  },
};
__shim.process = { argv: [], stdout: { write: function () {} }, exit: function () {}, env: {} };
global.Buffer = __Buffer;
// 影子 Buffer：模块代码里的裸 Buffer 必须解析到 shim（WebView 无 Node Buffer），
// 否则金标测试会漏到 Node 真 Buffer 上，掩盖垫片缺口。
var Buffer = __Buffer;

__define("mns.js", function (module, exports, require, __filename, __dirname) {
/**
 * mnsv2 → mns0101_/0201_/0301_ 100% 纯算法
 * ============================================================
 *
 *   sign = "mns0301_" + base64_custom( XOR( pack(fields), KEYSTREAM ) )
 *
 * 全部子过程均已还原为可读算法（无 VM / 无 __$c / 无补环境）：
 *   - pack        : 见 packPlaintext（TLV 字段布局）
 *   - ds 环境指纹 : envFp()  = [1, ver0^115, <14 固定环境常量>]
 *   - ds 设备签名 : dsSig()  = dsf(le64(ts)++md5(api+data)) 逐字节 XOR ver0
 *   - _dsf        : 自定义 4-lane ARX 哈希（见 dsf()）
 *   - encrypt     : XOR 固定密钥流（stream cipher）
 *   - base64      : 自定义字母表
 */
'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const ALPHABET = 'MfgqrsbcyzPQRStuvC7mn501HIJBo2DEFTKdeNOwxWXYZap89+/A4UVLhijkl63G';
const MNS0101_ALPHABET = 'NOPQRStuvwxWXYZabcyz012DEFTKLMdefghijkl4563GHIJBC7mnop89+/';

const KEYSTREAM = (() => {
  const j = JSON.parse(fs.readFileSync(path.join(__dirname, 'mns_keystreams.json'), 'utf8'));
  return [...Buffer.from(j.b64, 'base64')];
})();

const MNS0101_KEYSTREAM = (() => {
  const j = JSON.parse(fs.readFileSync(path.join(__dirname, 'mns_0101_keystream.json'), 'utf8'));
  return [...Buffer.from(j.b64, 'base64')];
})();

// envConst/envFpTail/loadts/seq/appId/deviceTag 属于页面、平台与会话状态，
// 不是算法常量。PC 与 Creator 共用本文件，平台层必须显式传入这些材料。

function md5Bytes(s) {
  return [...crypto.createHash('md5').update(String(s), 'utf8').digest()];
}
function le32(n) {
  n >>>= 0;
  return [n & 255, (n >>> 8) & 255, (n >>> 16) & 255, (n >>> 24) & 255];
}
function le64(n) {
  const out = [];
  let b = BigInt(n);
  for (let i = 0; i < 8; i++) { out.push(Number(b & 255n)); b >>= 8n; }
  return out;
}
function strBytes(s) {
  return [...Buffer.from(String(s), 'utf8')];
}
function base64Custom(bytes) {
  let out = '';
  for (let i = 0; i < bytes.length; i += 3) {
    const b0 = bytes[i], b1 = bytes[i + 1], b2 = bytes[i + 2];
    out += ALPHABET[b0 >> 2];
    out += ALPHABET[((b0 & 3) << 4) | ((b1 || 0) >> 4)];
    if (b1 === undefined) { out += '=='; break; }
    out += ALPHABET[((b1 & 15) << 2) | ((b2 || 0) >> 6)];
    if (b2 === undefined) { out += '='; break; }
    out += ALPHABET[b2 & 63];
  }
  return out;
}

// mns0101 使用自定义 Base58，而不是 0201/0301 的自定义 Base64。
// 前导零使用该字母表中的 `1` 表示。
function base58Custom(bytes) {
  let value = 0n;
  for (const byte of bytes) value = (value << 8n) | BigInt(byte);
  let body = '';
  while (value > 0n) {
    body = MNS0101_ALPHABET[Number(value % 58n)] + body;
    value /= 58n;
  }
  let zeros = 0;
  for (const byte of bytes) {
    if (byte !== 0) break;
    zeros++;
  }
  return '1'.repeat(zeros) + body;
}

function base58CustomDecode(text) {
  const reverse = new Map(Array.from(MNS0101_ALPHABET, (char, index) => [char, BigInt(index)]));
  let value = 0n;
  let offset = 0;
  while (offset < text.length && text[offset] === '1') offset++;
  for (let i = offset; i < text.length; i++) {
    const digit = reverse.get(text[i]);
    if (digit === undefined) throw new Error(`invalid mns0101 base58 character at ${i}`);
    value = value * 58n + digit;
  }
  const bytes = [];
  while (value > 0n) {
    bytes.unshift(Number(value & 255n));
    value >>= 8n;
  }
  return new Array(offset).fill(0).concat(bytes);
}

// --- ds 设备签名 _dsf：自定义 4-lane ARX 哈希 ---
function rotl(x, r) {
  return (((x << r) | 0) + (x >>> (32 - r))) | 0;
}
function dsf(data) {
  const len = data.length;
  const rd = (o) => (data[o] | (data[o + 1] << 8) | (data[o + 2] << 16) | (data[o + 3] << 24)) | 0;
  let h1 = (0x6d2b79f5 ^ len) | 0;
  let h2 = (0x1b873593 ^ (len << 8)) | 0;
  let h3 = (0x85ebca6b ^ (len << 16)) | 0;
  let h4 = (0xc2b2ae35 ^ (len << 24)) | 0;
  for (let i = 0; i + 8 <= len; i += 8) {
    const w0 = rd(i), w1 = rd(i + 4);
    h1 = rotl((((h1 + w0) | 0) ^ h3) | 0, 7);
    h2 = rotl((((h2 ^ w0) | 0) + h4) | 0, 11);
    h3 = rotl((((h3 + w1) | 0) ^ h1) | 0, 13);
    h4 = rotl((((h4 ^ w1) | 0) + h2) | 0, 17);
  }
  h1 = (h1 ^ len) | 0;
  h2 = (h2 ^ h1) | 0;
  h3 = (h3 + h2) | 0;
  h4 = (h4 ^ h3) | 0;
  h1 = rotl(h1, 9); h2 = rotl(h2, 13); h3 = rotl(h3, 17); h4 = rotl(h4, 19);
  h1 = (h1 + h3) | 0;
  h2 = (h2 ^ h4) | 0;
  h3 = (h3 + h1) | 0;
  h4 = (h4 ^ h2) | 0;
  const out = [];
  for (const h of [h1 >>> 0, h2 >>> 0, h3 >>> 0, h4 >>> 0]) {
    out.push(h & 255, (h >>> 8) & 255, (h >>> 16) & 255, (h >>> 24) & 255);
  }
  return out;
}

function envFp(versionKey, tail) {
  if (!Array.isArray(tail) || tail.length !== 14) {
    throw new Error('envFpTail required (14 bytes)');
  }
  return [1, versionKey ^ 115].concat(tail);
}

function packPlaintext(p) {
  if (p.loadts == null) throw new Error('mns loadts required');
  if (p.seq == null) throw new Error('mns seq required');
  if (p.envConst == null) throw new Error('mns envConst required');
  if (!Array.isArray(p.envFpTail) || p.envFpTail.length !== 14) {
    throw new Error('mns envFpTail required (14 bytes)');
  }
  const version = p.version >>> 0;
  const vKey = version & 255;
  const ts = p.ts;
  const loadts = p.loadts;
  const appId = String(p.appId || 'xhs-pc-web');
  const deviceTag = String(p.deviceTag || p.dsn || 'a3');
  const full = String(p.api) + (p.data || '');
  // md5FullBytes / md5UrlBytes are the u and p params from seccore_signv2 (already-computed md5s).
  // If not supplied, compute from api+data / api locally.
  const md5full = p.md5FullBytes ? [...p.md5FullBytes] : md5Bytes(full);
  const md5api = p.md5UrlBytes ? [...p.md5UrlBytes] : md5Bytes(p.api);
  // DS 脚本尚未就绪时浏览器的 window._dsn = "nop"，此阶段尾部直接放
  // MD5(url) XOR versionLowByte；DS 就绪后 PC/Creator 分别使用 a3/a1，
  // 尾部改为 _dsf(null, timestamp || MD5(url)) XOR versionLowByte。
  // Creator 的 _dsf 来自当前服务端 DS 程序，通过 dsSigBytes 显式传入；
  // 本地 dsf() 仅保留给已经完成静态提纯的平台路径。
  let rawDsSig;
  if (deviceTag === 'nop') {
    rawDsSig = md5api;
  } else if (Array.isArray(p.dsSigBytes) || ArrayBuffer.isView(p.dsSigBytes)) {
    rawDsSig = Array.from(p.dsSigBytes);
    if (rawDsSig.length !== 16) throw new Error('mns dsSigBytes must be 16 bytes');
  } else {
    rawDsSig = dsf(le64(ts).concat(md5api));
  }
  const dsSig = rawDsSig.map((b) => b ^ vKey);

  const out = [];
  const push = (a) => { for (const b of a) out.push(b); };
  push([121, 104, 96, 41]);
  push(le32(version));
  push(le64(ts));
  push(le64(loadts));
  push(le32(p.seq >>> 0)); // 请求序列号（会话自增计数器）
  push(le32(p.envConst >>> 0));
  push(le32(p.fullLen != null ? p.fullLen >>> 0 : Buffer.byteLength(full, 'utf8')));
  push(md5full.slice(0, 8).map((b) => b ^ vKey));
  push([p.a1.length]);
  push(strBytes(p.a1));
  const appBytes = strBytes(appId);
  if (appBytes.length > 255) throw new Error('mns appId too long');
  push([appBytes.length]);
  push(appBytes);
  push(envFp(vKey, p.envFpTail));
  const tagBytes = strBytes(deviceTag);
  if (tagBytes.length > 255) throw new Error('mns deviceTag too long');
  push([tagBytes.length]);
  push(tagBytes);
  push([16]);
  push(dsSig);
  return out;
}

// Helper: hex string → bytes array
function hexToBytes(hex) {
  const out = [];
  for (let i = 0; i < hex.length; i += 2) out.push(parseInt(hex.slice(i, i + 2), 16));
  return out;
}

// seccore_signv2 wrapper: matches browser's exact call convention.
// c = url + JSON.stringify(data) or url + data(string), u = md5(c) hex, p = md5(url) hex.
function seccoreSignV2({
  url, data, a1, ts, loadts, seq, version, envConst, envFpTail,
  appId = 'xhs-pc-web', deviceTag = 'a3',
}) {
  const c = String(url) + (data == null ? '' : (typeof data === 'object' ? JSON.stringify(data) : String(data)));
  const uHex = crypto.createHash('md5').update(c).digest('hex');
  const pHex = crypto.createHash('md5').update(String(url)).digest('hex');
  const md5FullBytes = hexToBytes(uHex);
  const md5UrlBytes = hexToBytes(pHex);
  return sign({
    api: url, data, a1, ts, loadts, seq, version, envConst, envFpTail,
    appId, deviceTag,
    md5FullBytes, md5UrlBytes,
    fullLen: Buffer.byteLength(c, 'utf8'),
  });
}

function sign(p) {
  const plain = packPlaintext(p);
  const cipher = plain.map((b, i) => b ^ KEYSTREAM[i]);
  return 'mns0301_' + base64Custom(cipher);
}

// mns0101：同一 144B pack → 固定密钥流 XOR → 自定义 Base58。
// 浏览器冷启动内容请求夹具已从显式 pack 输入 6/6 byte-exact。
function sign0101(p) {
  const plain = packPlaintext(p);
  if (plain.length > MNS0101_KEYSTREAM.length) {
    throw new Error(`mns0101 keystream too short: ${MNS0101_KEYSTREAM.length} < ${plain.length}`);
  }
  const cipher = plain.map((byte, index) => byte ^ MNS0101_KEYSTREAM[index]);
  return 'mns0101_' + base58Custom(cipher);
}

// ============================================================
// mns0201 档位：XXTEA(自定义 DELTA) + 同一自定义 base64 字母表
// ------------------------------------------------------------
// 从 vendor-dynamic VM 字节码静态还原（f_13448_141 管线）：
//   pack(144B) --toWordsLE(includeLength=true)--> 37 words
//   --XXTEA_encrypt(key, DELTA=1013904243)--> 37 words
//   --fromWordsLE(includeLength=false)--> 148 bytes
//   --base64_custom--> payload ；结果 = "mns0201_" + payload
// 真实密钥 = utf8("e6483ca2a1eed5e3")（VM 内 "e6"+"483c"+"a2a1eed5e3" 拼接）。
// 注意："99754106633f94d350db34d548d6091a" 是字节码里的诱饵（其派生值随即被覆盖）。
const MNS0201_DELTA = 1013904243; // 0x3C6EF373（非标准 golden-ratio）
const MNS0201_KEY_STR = 'e6483ca2a1eed5e3';

function mnsKeyWords() {
  let kb = [...Buffer.from(MNS0201_KEY_STR, 'utf8')];
  if (kb.length < 16) kb = kb.concat(new Array(16 - kb.length).fill(0));
  return toWordsLE(kb, false);
}
function toWordsLE(bytes, includeLength) {
  const e = bytes.length;
  let a = e >> 2;
  if (e & 3) a++;
  const t = includeLength ? new Array(a + 1).fill(0) : new Array(a).fill(0);
  if (includeLength) t[a] = e;
  for (let o = 0; o < e; o++) t[o >> 2] |= bytes[o] << ((o & 3) << 3);
  return t;
}
function fromWordsLE(r, includeLength) {
  let t = r.length, a = t << 2;
  if (includeLength) { const e = r[t - 1]; a -= 4; if (e < a - 3 || e > a) return null; a = e; }
  const f = new Array(a);
  for (let i = 0; i < a; i++) f[i] = (r[i >> 2] >>> ((i & 3) << 3)) & 0xff;
  return f;
}
function xxteaMx(sum, y, z, p, e, k) {
  return ((((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4))) ^ (((sum ^ y) + (k[(p & 3) ^ e] ^ z)))) | 0;
}
function xxteaEncrypt(v, k) {
  const n = v.length;
  if (n < 2) return v;
  const nn = n - 1;
  let rounds = Math.floor(6 + 52 / n);
  let sum = 0, y, z = v[nn], e, p;
  while (rounds-- > 0) {
    sum = (sum + MNS0201_DELTA) | 0;
    e = (sum >>> 2) & 3;
    for (p = 0; p < nn; p++) { y = v[p + 1]; z = v[p] = (v[p] + xxteaMx(sum, y, z, p, e, k)) | 0; }
    y = v[0]; z = v[nn] = (v[nn] + xxteaMx(sum, y, z, nn, e, k)) | 0;
  }
  return v;
}
function xxteaDecrypt(v, k) {
  const n = v.length;
  if (n < 2) return v;
  const nn = n - 1;
  let rounds = Math.floor(6 + 52 / n);
  let sum = (rounds * MNS0201_DELTA) | 0, y = v[0], z, e, p;
  while (sum !== 0) {
    e = (sum >>> 2) & 3;
    for (p = nn; p > 0; p--) { z = v[p - 1]; y = v[p] = (v[p] - xxteaMx(sum, y, z, p, e, k)) | 0; }
    z = v[nn]; y = v[0] = (v[0] - xxteaMx(sum, y, z, 0, e, k)) | 0;
    sum = (sum - MNS0201_DELTA) | 0;
  }
  return v;
}

function encrypt0201(pack) {
  const kw = mnsKeyWords();
  const dataWords = toWordsLE(pack, true);
  const cipherWords = xxteaEncrypt(dataWords.slice(), kw);
  return fromWordsLE(cipherWords, false);
}
function sign0201(p) {
  const plain = packPlaintext(p);
  return 'mns0201_' + base64Custom(encrypt0201(plain));
}

// 统一入口：按档位分发（0301 默认；0201 = XXTEA）
function signTier(p, tier) {
  switch (tier || '0301') {
    case '0101': return sign0101(p);
    case '0201': return sign0201(p);
    case '0301': return sign(p);
    default: throw new Error(`unsupported mns tier: ${tier}`);
  }
}

module.exports = {
  sign, sign0101, sign0201, signTier, packPlaintext, dsf, envFp,
  base58Custom, base58CustomDecode, base64Custom,
  KEYSTREAM, MNS0101_KEYSTREAM, ALPHABET, MNS0101_ALPHABET,
  encrypt0201, xxteaEncrypt, xxteaDecrypt, toWordsLE, fromWordsLE, mnsKeyWords,
  MNS0201_DELTA, MNS0201_KEY_STR, seccoreSignV2, hexToBytes, le64
};

});
__define("xs_common.js", function (module, exports, require, __filename, __dirname) {
/**
 * XHS Web 共用 X-S-Common 纯算（还原自 launcher XsCommon）：
 *   customB64( utf8( JSON.stringify({s0..x12}) ) )
 *   x9 = 变体 CRC32(b1)（多项式 0xedb88320，末值 ~c ^ 0xedb88320）
 * 无 VM / 无静态混淆 JS。
 */
'use strict';

const XS_ALPHABET = 'ZmserbBoHQtNP+wOcza/LpngG8yJq42KWYj0DSfdikx3VT16IlUAFM97hECvuRX5';

const _crcTable = (() => {
  const a = 0xedb88320;
  const s = new Array(256);
  for (let d = 0; d < 256; d++) {
    let r = d;
    for (let c = 8; c > 0; c--) r = 1 & r ? (r >>> 1) ^ a : r >>> 1;
    s[d] = r >>> 0;
  }
  return s;
})();

function gens9(str) {
  const a = 0xedb88320;
  let c = -1;
  for (let r = 0; r < str.length; r++) {
    c = _crcTable[(255 & c) ^ str.charCodeAt(r)] ^ (c >>> 8);
  }
  return (-1 ^ c ^ a) | 0; // signed int32，与 JS 一致
}

function encodeUtf8(e) {
  const r = encodeURIComponent(e), a = [];
  for (let i = 0; i < r.length; i++) { const ch = r.charAt(i); if (ch === '%') { a.push(parseInt(r.charAt(i + 1) + r.charAt(i + 2), 16)); i += 2; } else a.push(ch.charCodeAt(0)); }
  return a;
}
function b64Custom(e) {
  const A = XS_ALPHABET, a = e.length, c = a % 3, d = []; const s = 16383; let u = 0; const l = a - c;
  for (u = 0; u < l; u += s) { const end = u + s > l ? l : u + s; for (let k = u; k < end; k += 3) { const t = (e[k] << 16) + ((k + 1 < end ? e[k + 1] : 0) << 8) + (k + 2 < end ? e[k + 2] : 0); d.push(A[(t >> 18) & 63], A[(t >> 12) & 63], A[(t >> 6) & 63], A[t & 63]); } }
  if (c === 1) { const t = e[a - 1]; d.push(A[t >> 2] + A[(t << 4) & 63] + '=='); }
  else if (c === 2) { const t = (e[a - 2] << 8) + e[a - 1]; d.push(A[t >> 10] + A[(t >> 4) & 63] + A[(t << 2) & 63] + '='); }
  return d.join('');
}

function xsCommon(a1, xt, xs, dslPair, b1, options = {}) {
  // Login/security cold paths still send X-S-Common before the b1 collector
  // has produced a snapshot.  In that phase x8 is the empty string and x9 is
  // its normal CRC variant; an empty b1 is therefore a real browser state,
  // not a missing-value fallback.
  if (b1 == null) throw new Error('xsCommon: b1 必须显式传入（允许空字符串）');
  if (!dslPair) throw new Error('xsCommon: dslPair 必传');
  const b1Value = String(b1);
  const config = typeof options === 'string' ? { webBuild: options } : options;
  const webBuild = String(config.webBuild || '6.47.2');
  // 2026-07-25 浏览器实证（zones/seccallback X-S-Common 解码）：
  // b1 未采集阶段浏览器发 x8: null（JSON null）且 x9 = gens9('null')，
  // 不是空字符串。服务端两种都容忍，字节级对齐以浏览器为准。
  const coldB1 = b1Value === '';
  const d = {
    s0: 5,
    s1: '',
    x0: String(config.b1b1 || '1'),
    x1: String(config.signVersion || '4.4.3'),
    x2: String(config.platform || 'Windows'),
    x3: String(config.appId || 'xhs-pc-web'),
    x4: webBuild,
    x5: a1,
    x6: '',
    x7: '',
    x8: coldB1 ? null : b1Value,
    x9: gens9(coldB1 ? 'null' : b1Value),
    x10: Number(config.signCount || 0),
    x11: 'normal',
    x12: dslPair,
  };
  return b64Custom(encodeUtf8(JSON.stringify(d)));
}

module.exports = { xsCommon, gens9 };


});
__define("sign.js", function (module, exports, require, __filename, __dirname) {
/**
 * XHS Web 共用签名算法入口：mns.js + xs_common.js。
 * Creator 0101 可显式传入服务端 DS 程序，在隔离 VM 中调用其 _dsf；
 * 其余签名路径不依赖浏览器宿主。
 * seccore_signv2 兼容版：内部对 (url + JSON.stringify(data)) 求 md5 作为 u 与 md5(url) 作为 p，
 * 与浏览器 window.mnsv2 完全一致（逐字节对齐）。
 *
 * CLI: node sign.js @input.json    ({api,data,cookie,version?,now?,seq?,tier?})
 */
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const vm = require('vm');
const { signTier, hexToBytes } = require('mns.js');
const { xsCommon } = require('xs_common.js');

const ALPHABET_XS = 'ZmserbBoHQtNP+wOcza/LpngG8yJq42KWYj0DSfdikx3VT16IlUAFM97hECvuRX5';
function encodeUtf8(e) {
  const r = encodeURIComponent(e), a = [];
  for (let i = 0; i < r.length; i++) { const c = r.charAt(i); if (c === '%') { a.push(parseInt(r.charAt(i + 1) + r.charAt(i + 2), 16)); i += 2; } else a.push(c.charCodeAt(0)); }
  return a;
}
function b64XS(e) {
  const a = e.length, c = a % 3, d = []; let u = 0; const l = a - c, s = 16383;
  for (u = 0; u < l; u += s) { const end = u + s > l ? l : u + s; for (let k = u; k < end; k += 3) { const t = (e[k] << 16) + ((k + 1 < end ? e[k + 1] : 0) << 8) + (k + 2 < end ? e[k + 2] : 0); d.push(ALPHABET_XS[(t >> 18) & 63], ALPHABET_XS[(t >> 12) & 63], ALPHABET_XS[(t >> 6) & 63], ALPHABET_XS[t & 63]); } }
  if (c === 1) { const t = e[a - 1]; d.push(ALPHABET_XS[t >> 2] + ALPHABET_XS[(t << 4) & 63] + '=='); }
  else if (c === 2) { const t = (e[a - 2] << 8) + e[a - 1]; d.push(ALPHABET_XS[t >> 10] + ALPHABET_XS[(t >> 4) & 63] + ALPHABET_XS[(t << 2) & 63] + '='); }
  return d.join('');
}

function parseWebSsk(value, appId) {
  let storage = value;
  if (typeof storage === 'string') {
    if (!storage) return null;
    try { storage = JSON.parse(storage); } catch (_) { return null; }
  }
  if (!storage || typeof storage !== 'object') return null;
  const encoded = storage[appId];
  if (typeof encoded !== 'string' || !encoded) return null;
  const raw = Buffer.from(encoded, 'base64');
  return raw.length >= 32 ? raw : null;
}

function buildEncSskSign(md5FullBytes, webSsk, appId, options = {}) {
  const secret = parseWebSsk(webSsk, appId);
  if (!secret) return null;
  const nonce = Buffer.allocUnsafe(4);
  const randomValue = options.sskRandom != null
    ? (Number(options.sskRandom) >>> 0)
    : crypto.randomBytes(4).readUInt32LE(0);
  const timestamp = options.sskTimestamp != null
    ? (Number(options.sskTimestamp) >>> 0)
    : (Date.now() >>> 0);
  nonce.writeUInt32LE((timestamp + randomValue) >>> 0, 0);
  const digest = crypto.createHash('sha1').update(Buffer.concat([secret, nonce])).digest();
  const encSsk = Buffer.concat([nonce, digest, secret.subarray(32)]);
  const encSskSign = crypto.createHash('sha1')
    .update(Buffer.concat([Buffer.from(md5FullBytes), encSsk]))
    .digest('base64');
  return {
    encSskSign: { [appId]: encSskSign },
    encSsk: { [appId]: encSsk.toString('base64') },
  };
}

function encodeXs(x3, x4Value, options = {}) {
  // Browser's seccore_signv2 always sets x4 = (data ? typeof data : '')
  // For empty data (GET), x4 = ''; for object data, x4 = 'object'; for string data, x4 = 'string'
  const signObj = {
    x0: String(options.signVersion || '4.4.3'),
    x1: String(options.appId || 'xhs-pc-web'),
    x2: String(options.platform || 'Windows'),
    x3,
    x4: x4Value,
  };
  // Current PC envelopes append x5 and, after webSsk activation, x6/x7.
  // Creator 4.3.6 envelopes stop at x4.
  if (options.x5 != null) signObj.x5 = String(options.x5);
  if (options.x6 != null) signObj.x6 = options.x6;
  if (options.x7 != null) signObj.x7 = options.x7;
  return 'XYS_' + b64XS(encodeUtf8(JSON.stringify(signObj)));
}

function runDsfProgram(program, inputBytes, timeoutMs = 10000) {
  const source = String(program || '');
  if (!source || (!source.includes('__$c') && !source.includes('_dsf'))) {
    throw new Error('Creator DS program is missing or invalid');
  }
  const sandbox = {};
  sandbox.global = sandbox;
  vm.runInNewContext(source, sandbox, { timeout: Number(timeoutMs) });
  if (typeof sandbox._dsf !== 'function') {
    throw new Error('Creator DS program did not export _dsf');
  }
  const output = Array.from(sandbox._dsf(null, Array.from(inputBytes || [])) || []);
  if (
    output.length !== 16
    || output.some(byte => !Number.isInteger(byte) || byte < 0 || byte > 255)
  ) {
    throw new Error('Creator _dsf output must be 16 bytes');
  }
  return output;
}

function signFull(input) {
  input = input || {};
  // 生产模式：cookie 必须从 CLI 传入；不再读磁盘兜底
  if (!input.cookie) throw new Error('cookie required (must be passed via CLI JSON)');
  const cookie = input.cookie;
  const cookieMap = Object.fromEntries(cookie.split(/;\s*/).map((part) => {
    const offset = part.indexOf('=');
    return offset < 0 ? [part, ''] : [part.slice(0, offset), part.slice(offset + 1)];
  }));
  const a1 = (cookie.match(/a1=([^;]*)/) || [])[1] || '';
  if (!a1) throw new Error('cookie a1 required');
  const now = input.now != null ? Number(input.now) : Date.now();
  const version = input.version != null ? (input.version >>> 0) : (Math.floor(Math.random() * 0x100000000) >>> 0);
  const loadts = input.loadts != null ? Number(input.loadts) : Number(cookieMap.loadts);
  if (!Number.isFinite(loadts)) throw new Error('loadts required (input.loadts or cookie loadts)');
  if (input.seq == null) throw new Error('seq required from PcDeviceProfile session state');
  if (input.envConst == null) throw new Error('envConst required from PcDeviceProfile stage state');
  if (!Array.isArray(input.envFpTail) || input.envFpTail.length !== 14) {
    throw new Error('envFpTail required from PcDeviceProfile stage state (14 bytes)');
  }
  // seccore_signv2 receives `data` as JS object or string (not stringified for u calc)
  // c = url + JSON.stringify(data) for objects, or url + data for strings
  const dataIn = input.data;
  const dataStr = dataIn == null ? '' : (typeof dataIn === 'object' ? JSON.stringify(dataIn) : String(dataIn));
  const c = String(input.api) + dataStr;
  // x4 label per browser: object data → 'object', string data (non-empty) → 'string', else ''
  const x4Label = dataIn == null || dataIn === '' ? '' : (typeof dataIn === 'object' ? 'object' : 'string');
  const md5Full = hexToBytes(crypto.createHash('md5').update(c).digest('hex'));
  const md5Url = hexToBytes(crypto.createHash('md5').update(String(input.api)).digest('hex'));
  const fullLen = Buffer.byteLength(c, 'utf8');
  const tier = input.tier || '0301';
  const deviceTag = String(input.deviceTag || input.dsn || 'a3');
  let dsSigBytes = input.dsSigBytes;
  // Creator's Python adapter historically called this context field
  // ``dsProgram`` while the standalone signer used ``dsfProgram``.  Accept
  // both spellings so the server-issued _dsf program is actually executed
  // for mns0101/a1 requests instead of silently falling back to the static
  // placeholder hash.
  const dsfProgram = input.dsfProgram || input.dsProgram;
  if (deviceTag !== 'nop' && !dsSigBytes && dsfProgram) {
    const dsfInput = [];
    let timestamp = BigInt(now);
    for (let index = 0; index < 8; index++) {
      dsfInput.push(Number(timestamp & 255n));
      timestamp >>= 8n;
    }
    dsfInput.push(...md5Url);
    dsSigBytes = runDsfProgram(dsfProgram, dsfInput, input.dsfTimeoutMs);
  }
  const x3 = signTier({
    api: input.api,
    data: dataStr,
    a1,
    appId: String(input.appId || 'xhs-pc-web'),
    deviceTag,
    dsSigBytes,
    version,
    ts: now,
    loadts,
    seq: input.seq,
    envConst: input.envConst,
    envFpTail: input.envFpTail,
    md5FullBytes: md5Full,
    md5UrlBytes: md5Url,
    fullLen,
  }, tier);
  const xsOptions = { ...input };
  const appId = String(input.appId || 'xhs-pc-web');
  if (appId === 'xhs-pc-web') {
    xsOptions.x5 = Buffer.from(md5Full).toString('hex');
    const ssk = buildEncSskSign(md5Full, input.webSsk, appId, input);
    if (ssk) {
      xsOptions.x6 = ssk.encSskSign;
      xsOptions.x7 = ssk.encSsk;
    } else {
      delete xsOptions.x6;
      delete xsOptions.x7;
    }
  } else {
    delete xsOptions.x5;
    delete xsOptions.x6;
    delete xsOptions.x7;
  }
  const xs = encodeXs(x3, x4Label, xsOptions);
  // Browser builds MNS first, then samples Date.now() again for X-t.
  const xt = input.xt != null ? String(input.xt) : String(Date.now());
  const out = { x3, xs, xt, len: x3.length, prefix: x3.slice(0, 12), tier, pure: true };
  // X-S-Common 纯算（需 b1 + dslPair）
  const b1 = input.b1 == null ? '' : String(input.b1);
  const dslPairRaw = input.dslPair == null ? input.dsl_pair : input.dslPair;
  const dslPair = dslPairRaw == null ? '' : String(dslPairRaw);
  if (dslPair) {
    const webBuild = String(input.webBuild || cookieMap.webBuild || '');
    if (!webBuild) throw new Error('webBuild required (input.webBuild or cookie webBuild)');
    out.xs_common = xsCommon(a1, xt, xs, dslPair, b1, {
      b1b1: input.b1b1,
      signVersion: input.signVersion,
      platform: input.platform,
      appId: input.appId,
      webBuild,
      signCount: input.signCount,
    });
  }
  return out;
}

function main() {
  const arg = process.argv[2];
  const raw = arg && arg.startsWith('@') ? fs.readFileSync(arg.slice(1), 'utf8') : arg;
  const input = JSON.parse(raw);
  process.stdout.write(JSON.stringify(signFull(input)) + '\n');
}

if (require.main === module) main();

module.exports = {
  signFull, encodeXs, encodeUtf8, b64XS, runDsfProgram,
  parseWebSsk, buildEncSskSign, main,
};


});
__define("b1.js", function (module, exports, require, __filename, __dirname) {
'use strict';

const fs = require('fs');

const B1_RC4_KEY = 'xhswebmplfbt';
const B1_BASE64_ALPHABET = 'ZmserbBoHQtNP+wOcza/LpngG8yJq42KWYj0DSfdikx3VT16IlUAFM97hECvuRX5';
const MINI_FIELD_KEYS = Object.freeze([
  'x33', 'x34', 'x35', 'x36', 'x37', 'x38', 'x39',
  'x42', 'x43', 'x44', 'x45', 'x46',
  'x48', 'x49', 'x50', 'x51', 'x52', 'x82', 'x84',
]);
const DEFAULT_X37 = '0|0|0|0|0|0|0|0|0|1|0|0|1|0|0|0|0|1|1|0|0|0|0|0';
const DEFAULT_X38 = '0|0|1|0|1|0|0|0|0|0|1|0|1|0|1|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0';
const DEFAULT_X82 = '1|_BHjFmfUMEtxhI|_AUuXfEG27Xa3x|__xhsPendingNotePoint25300ReportMap|__xhsReportedNotePoint25300RecordMap|setImDebugMode|anti_hp_sign_config|__rap_app_id__|__rap_report__|__rap_last_sign_cost__|__rap_last_transform_cost__|__rap_hijack_installed__|__ed1a7dddf7c818e4bd|__hp_xhs_search_input_state__|__h';
const DEFAULT_X39 = '22';
const DEFAULT_X45 = '__SEC_CAV__1-1-1-1-1|';
const DEFAULT_X50 = '131,88,103';

function orderedMiniFields(input) {
  const out = {};
  for (const key of MINI_FIELD_KEYS) {
    if (Object.prototype.hasOwnProperty.call(input || {}, key)) out[key] = input[key];
  }
  return out;
}

function rc4BinaryString(plainText, key = B1_RC4_KEY) {
  const state = Array.from({ length: 256 }, (_, i) => i);
  let j = 0;
  for (let i = 0; i < 256; i++) {
    j = (j + state[i] + key.charCodeAt(i % key.length)) & 0xff;
    [state[i], state[j]] = [state[j], state[i]];
  }

  let i = 0;
  j = 0;
  let encrypted = '';
  for (let offset = 0; offset < plainText.length; offset++) {
    i = (i + 1) & 0xff;
    j = (j + state[i]) & 0xff;
    [state[i], state[j]] = [state[j], state[i]];
    const keyByte = state[(state[i] + state[j]) & 0xff];
    encrypted += String.fromCharCode(plainText.charCodeAt(offset) ^ keyByte);
  }
  return encrypted;
}

function customBase64(bytes, alphabet = B1_BASE64_ALPHABET) {
  let out = '';
  for (let offset = 0; offset < bytes.length; offset += 3) {
    const a = bytes[offset];
    const hasB = offset + 1 < bytes.length;
    const hasC = offset + 2 < bytes.length;
    const b = hasB ? bytes[offset + 1] : 0;
    const c = hasC ? bytes[offset + 2] : 0;
    out += alphabet[a >>> 2];
    out += alphabet[((a & 0x03) << 4) | (b >>> 4)];
    out += hasB ? alphabet[((b & 0x0f) << 2) | (c >>> 6)] : '=';
    out += hasC ? alphabet[c & 0x3f] : '=';
  }
  return out;
}

function customBase64Decode(text, alphabet = B1_BASE64_ALPHABET) {
  const reverse = new Map(Array.from(alphabet, (char, index) => [char, index]));
  const bytes = [];
  for (let offset = 0; offset < text.length; offset += 4) {
    const a = reverse.get(text[offset]);
    const b = reverse.get(text[offset + 1]);
    const c = text[offset + 2] === '=' ? 0 : reverse.get(text[offset + 2]);
    const d = text[offset + 3] === '=' ? 0 : reverse.get(text[offset + 3]);
    if ([a, b, c, d].some(x => x === undefined)) throw new Error('invalid b1 base64 character');
    bytes.push((a << 2) | (b >>> 4));
    if (text[offset + 2] !== '=') bytes.push(((b & 0x0f) << 4) | (c >>> 2));
    if (text[offset + 3] !== '=') bytes.push(((c & 0x03) << 6) | d);
  }
  return Uint8Array.from(bytes);
}

function encodeB1FromMini(miniFields) {
  const plainText = JSON.stringify(orderedMiniFields(miniFields));
  const encrypted = rc4BinaryString(plainText);
  return customBase64(Buffer.from(encrypted, 'utf8'));
}

function decodeB1(b1) {
  const encrypted = Buffer.from(customBase64Decode(b1)).toString('utf8');
  const plainText = rc4BinaryString(encrypted);
  return JSON.parse(plainText);
}

function formatTelemetry(options = {}) {
  const now = Number(options.now ?? Date.now());
  const timeOrigin = Number(options.timeOrigin ?? (now - Number(options.sessionAgeMs ?? 2151.1)));
  const activeProfile = options.profile === 'active' || options.profile === 'legacy-active';
  const mouse = activeProfile
    ? { me: 50, mm: 50, md: 1, mu: 1, c: 1, ...(options.mouse || {}) }
    : { ...(options.mouse || {}) };
  const page = activeProfile
    ? { ulr: 1, ps: 1, f: 3, b: 3, vc: 0, rs: 1, sc: 0, ...(options.page || {}) }
    : { ulr: 2, ps: 1, ...(options.page || {}) };
  const state = { h: 0, f: 1, kr: 0, ...(options.state || {}) };
  const featureInput = options.features || {};
  const features = activeProfile ? {
    ae: 3.3656015629507223,
    ak: 6.231183732330187,
    cdr: 0.4096814442007536,
    bf: { ar: 0.6210873146622735, fr: 7.158084478545331 },
    fi: Number(options.firstInteraction ?? 2151.1),
    ...featureInput,
  } : {
    ae: null,
    ak: null,
    cdr: null,
    bf: null,
    fi: null,
    ...featureInput,
  };
  const keyboard = options.keyboard || {};
  const mapText = value => Object.entries(value)
    .map(([key, item]) => `${key}:${item}`)
    .join(',');
  const bfText = features.bf == null
    ? 'null'
    : `{ar:${features.bf.ar},fr:${features.bf.fr}}`;
  return `{mt:{to:${timeOrigin}},m:{${mapText(mouse)}},k:{${mapText(keyboard)}},p:{${mapText(page)}},st:{${mapText(state)}},ft:{ae:${features.ae},ak:${features.ak},cdr:${features.cdr},bf:${bfText},fi:${features.fi}}}`;
}

// 默认构造“冷启动/低交互”profile。b1 是环境快照，不是稳定设备 ID：
// x36/x39/x44/x50/x82/x84 会随页面、浏览器能力和交互状态变化。
// 如需和某一条浏览器样本逐字节相等，必须把该样本解出的字段通过 overrides 传入。
function buildMiniFields(options = {}) {
  const now = Number(options.now ?? Date.now());
  const windowKeysRaw = Array.isArray(options.windowKeys)
    ? `1|${options.windowKeys.join('|')}`
    : (options.x82 ?? DEFAULT_X82);
  // 浏览器 collector 对页面全局名快照最多保留 300 个字符。
  const windowKeys = String(windowKeysRaw).slice(0, 300);
  const fields = {
    x33: '0',
    x34: '0',
    x35: '0',
    x36: String(options.frameCount ?? 2),
    x37: DEFAULT_X37,
    x38: DEFAULT_X38,
    // x39 不是 window/global 数量。Creator 4.3.6 的发布页采集器通常
    // 落在 16-22；纯算层把它作为显式环境输入，历史 fixture 可覆盖。
    x39: String(options.x39 ?? DEFAULT_X39),
    // Creator's current 4.3.6 collector reports 3.5.6.  Captured fixture
    // callers can still override x42 explicitly for historical replays.
    x42: String(options.sdkVersion ?? '3.5.6'),
    x43: String(options.canvasFingerprint ?? 'Canvas not supported'),
    x44: String(now),
    x45: String(options.secCanvas ?? DEFAULT_X45),
    x46: String(options.webdriver ?? false),
    x48: String(options.x48 ?? ''),
    x49: String(options.x49 ?? '{list:[],type:}'),
    // x50 是初始化期缓存的三元素浏览器能力向量，不是 global 数量。
    x50: String(options.x50 ?? options.globalCount ?? DEFAULT_X50),
    x51: String(options.x51 ?? ''),
    x52: String(options.x52 ?? ''),
    x82: String(windowKeys),
    x84: String(options.x84 ?? formatTelemetry({ ...options.telemetry, now })),
  };
  return Object.assign(fields, options.overrides || {});
}

function generateB1(options = {}) {
  const mini = buildMiniFields(options);
  return { b1: encodeB1FromMini(mini), mini };
}

function main(argv = process.argv) {
  const arg = argv[2];
  if (arg === '--generate') {
    const options = argv[3] ? JSON.parse(argv[3]) : {};
    process.stdout.write(JSON.stringify(generateB1(options)) + '\n');
    return;
  }
  if (arg === '--decode') {
    process.stdout.write(JSON.stringify(decodeB1(argv[3] || ''), null, 2) + '\n');
    return;
  }
  if (!arg) {
    process.stdout.write(JSON.stringify(generateB1()) + '\n');
    return;
  }
  const input = arg.startsWith('@')
    ? JSON.parse(fs.readFileSync(arg.slice(1), 'utf8'))
    : JSON.parse(arg);
  process.stdout.write(encodeB1FromMini(input) + '\n');
}

if (require.main === module) main();

module.exports = {
  B1_RC4_KEY,
  B1_BASE64_ALPHABET,
  MINI_FIELD_KEYS,
  orderedMiniFields,
  rc4BinaryString,
  customBase64,
  customBase64Decode,
  encodeB1FromMini,
  decodeB1,
  formatTelemetry,
  buildMiniFields,
  generateB1,
  main,
};

});
__define("deflate.js", function (module, exports, require, __filename, __dirname) {
'use strict';
// Full DEFLATE encoder matching browser: simple greedy LZ77 + dynamic Huffman
// Verify byte-exact against browser samples

const LEN_BASE = [3,4,5,6,7,8,9,10,11,13,15,17,19,23,27,31,35,43,51,59,67,83,99,115,131,163,195,227,258];
const LEN_EXTRA = [0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0];
const DIST_BASE = [1,2,3,4,5,7,9,13,17,25,33,49,65,97,129,193,257,385,513,769,1025,1537,2049,3073,4097,6145,8193,12289,16385,24577];
const DIST_EXTRA = [0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13];
const CLC_ORDER = [16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15];

function lenToCode(l) {
  for (let i = 0; i < LEN_BASE.length; i++) {
    if (l >= LEN_BASE[i] && (i === LEN_BASE.length - 1 || l < LEN_BASE[i + 1])) {
      return { code: 257 + i, extra: LEN_EXTRA[i], extraBits: l - LEN_BASE[i] };
    }
  }
  throw new Error('bad len ' + l);
}

function distToCode(d) {
  for (let i = 0; i < DIST_BASE.length; i++) {
    if (d >= DIST_BASE[i] && (i === DIST_BASE.length - 1 || d < DIST_BASE[i + 1])) {
      return { code: i, extra: DIST_EXTRA[i], extraBits: d - DIST_BASE[i] };
    }
  }
  throw new Error('bad dist ' + d);
}

function lz77Tokenize(input, maxMatchLen = 5, minMatchLen = 3, windowSize = 128) {
  const tokens = [];
  let pos = 0;
  while (pos < input.length) {
    let bestLen = 0, bestDist = 0;
    const searchStart = Math.max(0, pos - windowSize);
    for (let back = pos - 1; back >= searchStart; back--) {
      const dist = pos - back;
      let len = 0;
      const maxLen = Math.min(maxMatchLen, input.length - pos);
      while (len < maxLen && input[back + len] === input[pos + len]) len++;
      if (len >= minMatchLen && len > bestLen) {
        bestLen = len; bestDist = dist;
        if (len === maxMatchLen) break;
      }
    }
    if (bestLen >= minMatchLen) {
      tokens.push({ t: 'M', len: bestLen, dist: bestDist });
      pos += bestLen;
    } else {
      tokens.push({ t: 'L', v: input[pos] });
      pos++;
    }
  }
  tokens.push({ t: 'END' });
  return tokens;
}

// Standard Huffman with length limit — package-merge would be more accurate, but zlib uses a specific heap-based algorithm
function buildHuffmanLengths(freqs, maxBits = 15) {
  // Build heap of (freq, sym) leaves; construct Huffman tree
  const n = freqs.length;
  const nodes = [];
  for (let i = 0; i < n; i++) if (freqs[i] > 0) nodes.push({ freq: freqs[i], sym: i, left: null, right: null });
  if (nodes.length === 0) return new Array(n).fill(0);
  if (nodes.length === 1) {
    const lens = new Array(n).fill(0);
    lens[nodes[0].sym] = 1;
    return lens;
  }
  // Build tree
  const heap = nodes.slice();
  const cmp = (a, b) => a.freq - b.freq;
  heap.sort(cmp);
  while (heap.length > 1) {
    const a = heap.shift();
    const b = heap.shift();
    const parent = { freq: a.freq + b.freq, sym: -1, left: a, right: b };
    let idx = 0;
    while (idx < heap.length && heap[idx].freq <= parent.freq) idx++;
    heap.splice(idx, 0, parent);
  }
  const lens = new Array(n).fill(0);
  const depths = new Array(n).fill(0);
  function walk(node, depth) {
    if (node.sym >= 0) { lens[node.sym] = depth || 1; depths[node.sym] = depth || 1; return; }
    walk(node.left, depth + 1);
    walk(node.right, depth + 1);
  }
  walk(heap[0], 0);

  // Enforce max bits limit（pako/zlib gen_bitlen 溢出修正，2026-07-25 移植）
  let overflow = 0;
  for (let i = 0; i < n; i++) if (lens[i] > maxBits) overflow++;
  if (overflow === 0) return lens;

  const bl_count = new Array(maxBits + 1).fill(0);
  for (let i = 0; i < n; i++) {
    if (lens[i]) bl_count[Math.min(lens[i], maxBits)]++;
  }
  do {
    let bits = maxBits - 1;
    while (bl_count[bits] === 0) bits--;
    bl_count[bits]--;
    bl_count[bits + 1] += 2;
    bl_count[maxBits]--;
    overflow -= 2;
  } while (overflow > 0);

  // 按 pako 的 smaller() 顺序（freq 升序、depth 升序、sym 升序）把叶子排好；
  // pako 的 heap 尾删序使最长码从最低频叶子开始分配（heap[--h] 从最小端扫）。
  const leaves = nodes
    .map(node => ({ sym: node.sym, freq: node.freq, depth: depths[node.sym] }))
    .sort((a, b) => (a.freq - b.freq) || (a.depth - b.depth) || (a.sym - b.sym));
  const out = new Array(n).fill(0);
  let h = 0;
  for (let bits = maxBits; bits !== 0; bits--) {
    let cnt = bl_count[bits];
    while (cnt !== 0) {
      const leaf = leaves[h++];
      out[leaf.sym] = bits;
      cnt--;
    }
  }
  return out;
}

// Canonical Huffman codes from lengths
function buildCanonical(lens) {
  const maxLen = Math.max(...lens, 0);
  const bl_count = new Array(maxLen + 1).fill(0);
  for (const l of lens) if (l) bl_count[l]++;
  const next_code = new Array(maxLen + 1).fill(0);
  let code = 0;
  for (let bits = 1; bits <= maxLen; bits++) {
    code = (code + bl_count[bits - 1]) << 1;
    next_code[bits] = code;
  }
  const codes = new Array(lens.length).fill(0);
  for (let n = 0; n < lens.length; n++) {
    if (lens[n]) {
      codes[n] = next_code[lens[n]];
      next_code[lens[n]]++;
    }
  }
  return codes;
}

class BitWriter {
  constructor() { this.chunks = []; this.buf = 0; this.nbits = 0; }
  writeBits(val, n) {
    this.buf |= (val << this.nbits) >>> 0;
    this.nbits += n;
    while (this.nbits >= 8) {
      this.chunks.push(this.buf & 0xff);
      this.buf >>>= 8;
      this.nbits -= 8;
    }
  }
  writeCode(code, len) {
    // DEFLATE Huffman codes emitted MSB-first
    let rev = 0;
    for (let i = 0; i < len; i++) {
      rev |= (((code >> (len - 1 - i)) & 1) << i);
    }
    this.writeBits(rev, len);
  }
  flush() {
    if (this.nbits > 0) {
      this.chunks.push(this.buf & 0xff);
      this.buf = 0; this.nbits = 0;
    }
    return Buffer.from(this.chunks);
  }
}

// Encode code-length sequence with RLE for the code-length alphabet
function rleCodeLens(lens) {
  const rle = [];
  let i = 0;
  while (i < lens.length) {
    const cur = lens[i];
    let run = 1;
    while (i + run < lens.length && lens[i + run] === cur) run++;
    if (cur === 0) {
      // Encode run of zeros
      while (run >= 11) {
        const r = Math.min(run, 138);
        rle.push({ sym: 18, extra: r - 11, extraBits: 7 });
        run -= r;
      }
      while (run >= 3) {
        const r = Math.min(run, 10);
        rle.push({ sym: 17, extra: r - 3, extraBits: 3 });
        run -= r;
      }
      while (run > 0) { rle.push({ sym: 0 }); run--; }
    } else {
      // Emit first, then use 16 (repeat prev) for subsequent
      rle.push({ sym: cur });
      run--;
      while (run >= 3) {
        const r = Math.min(run, 6);
        rle.push({ sym: 16, extra: r - 3, extraBits: 2 });
        run -= r;
      }
      while (run > 0) { rle.push({ sym: cur }); run--; }
    }
    i += (run === 0 ? (i + run + 1 === i ? 1 : 0) : 0);  // this loop advance
    // Actually: re-set i to start of next different value
    let jmp = 1;
    while (i + jmp < lens.length && lens[i + jmp] === cur) jmp++;
    i += jmp;
  }
  return rle;
}

function encodeDeflate(input) {
  const tokens = lz77Tokenize(input);

  // Count frequencies
  const litFreq = new Array(286).fill(0);
  const distFreq = new Array(30).fill(0);
  litFreq[256] = 1; // END
  for (const t of tokens) {
    if (t.t === 'L') litFreq[t.v]++;
    else if (t.t === 'M') {
      const lc = lenToCode(t.len);
      litFreq[lc.code]++;
      const dc = distToCode(t.dist);
      distFreq[dc.code]++;
    }
  }

  // Build Huffman code lengths
  const litLens = buildHuffmanLengths(litFreq, 15);
  const distLens = buildHuffmanLengths(distFreq, 15);

  // Find HLIT, HDIST
  let hlitMax = 256;
  for (let i = 285; i > 256; i--) if (litLens[i]) { hlitMax = i; break; }
  const HLIT = hlitMax - 256;  // HLIT + 257 = alphabet size
  let hdistMax = 0;
  for (let i = 29; i > 0; i--) if (distLens[i]) { hdistMax = i; break; }
  const HDIST = hdistMax;  // HDIST + 1 = alphabet size

  // Trim to actual size
  const litLensTrim = litLens.slice(0, HLIT + 257);
  const distLensTrim = distLens.slice(0, HDIST + 1);

  // RLE encode the lengths
  const combined = litLensTrim.concat(distLensTrim);
  const rle = rleCodeLens(combined);

  // Build code-length alphabet Huffman
  const clenFreq = new Array(19).fill(0);
  for (const r of rle) clenFreq[r.sym]++;
  const clenLens = buildHuffmanLengths(clenFreq, 7);

  // Find HCLEN (minimum needed by CLC_ORDER)
  let HCLEN = 0;
  for (let i = 18; i >= 4; i--) if (clenLens[CLC_ORDER[i]]) { HCLEN = i - 3; break; }

  // Build canonical codes for all three Huffman tables
  const litCodes = buildCanonical(litLensTrim);
  const distCodes = buildCanonical(distLensTrim);
  const clenCodes = buildCanonical(clenLens);

  // Write DEFLATE stream
  const bw = new BitWriter();
  bw.writeBits(1, 1);       // BFINAL
  bw.writeBits(2, 2);       // BTYPE=10 (dynamic)
  bw.writeBits(HLIT, 5);
  bw.writeBits(HDIST, 5);
  bw.writeBits(HCLEN, 4);
  for (let i = 0; i < HCLEN + 4; i++) bw.writeBits(clenLens[CLC_ORDER[i]], 3);

  // Write encoded code lengths using clen Huffman
  for (const r of rle) {
    bw.writeCode(clenCodes[r.sym], clenLens[r.sym]);
    if (r.sym === 16) bw.writeBits(r.extra, 2);
    else if (r.sym === 17) bw.writeBits(r.extra, 3);
    else if (r.sym === 18) bw.writeBits(r.extra, 7);
  }

  // Write actual data
  for (const t of tokens) {
    if (t.t === 'L') {
      bw.writeCode(litCodes[t.v], litLensTrim[t.v]);
    } else if (t.t === 'M') {
      const lc = lenToCode(t.len);
      bw.writeCode(litCodes[lc.code], litLensTrim[lc.code]);
      if (lc.extra) bw.writeBits(lc.extraBits, lc.extra);
      const dc = distToCode(t.dist);
      bw.writeCode(distCodes[dc.code], distLensTrim[dc.code]);
      if (dc.extra) bw.writeBits(dc.extraBits, dc.extra);
    } else if (t.t === 'END') {
      bw.writeCode(litCodes[256], litLensTrim[256]);
    }
  }

  return bw.flush();
}

// Wrap in gzip format
function encodeGzip(input, mtime = 0) {
  const deflate = encodeDeflate(input);
  const gzip = Buffer.alloc(10 + deflate.length + 8);
  gzip[0] = 0x1f; gzip[1] = 0x8b;
  gzip[2] = 0x08;    // CM = DEFLATE
  gzip[3] = 0x00;    // FLG = 0
  gzip.writeUInt32LE(mtime, 4);  // MTIME
  gzip[8] = 0x00;    // XFL
  gzip[9] = 0x03;    // OS = Unix
  deflate.copy(gzip, 10);
  const crc = require('zlib').crc32(input);
  gzip.writeUInt32LE(crc, 10 + deflate.length);
  gzip.writeUInt32LE(input.length & 0xffffffff, 14 + deflate.length);
  return gzip;
}

module.exports = { encodeDeflate, encodeGzip, lz77Tokenize };

});
__define("aes_encrypt.js", function (module, exports, require, __filename, __dirname) {
'use strict';
// AES-128 single-block encrypt with swappable S-box (standard MixColumns/ShiftRows/Rcon)
function mul(a, b) { let r = 0; for (let i = 0; i < 8; i++) { if (b & 1) r ^= a; const hi = a & 0x80; a = (a << 1) & 0xff; if (hi) a ^= 0x1b; b >>= 1; } return r; }
function keyExpand(key, sbox) {
  const Nk = 4, Nr = 10; const w = [];
  for (let i = 0; i < Nk; i++) w[i] = [key[4 * i], key[4 * i + 1], key[4 * i + 2], key[4 * i + 3]];
  const rcon = [0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36];
  for (let i = Nk; i < 4 * (Nr + 1); i++) {
    let t = w[i - 1].slice();
    if (i % Nk === 0) { t = [t[1], t[2], t[3], t[0]].map((x) => sbox[x]); t[0] ^= rcon[i / Nk - 1]; }
    w[i] = w[i - Nk].map((x, j) => x ^ t[j]);
  }
  return w;
}
function aesEncBlock(pt, key, sbox) {
  const Nr = 10; const w = keyExpand(key, sbox);
  const s = new Array(16);
  for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) s[r * 4 + c] = pt[c * 4 + r];
  function addRK(round) { for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) s[r * 4 + c] ^= w[round * 4 + c][r]; }
  addRK(0);
  for (let round = 1; round <= Nr; round++) {
    for (let i = 0; i < 16; i++) s[i] = sbox[s[i]];
    const t = s.slice(); for (let r = 1; r < 4; r++) for (let c = 0; c < 4; c++) s[r * 4 + c] = t[r * 4 + ((c + r) % 4)];
    if (round < Nr) { const o = s.slice(); for (let c = 0; c < 4; c++) { const a0 = o[c], a1 = o[4 + c], a2 = o[8 + c], a3 = o[12 + c]; s[c] = mul(a0, 2) ^ mul(a1, 3) ^ a2 ^ a3; s[4 + c] = a0 ^ mul(a1, 2) ^ mul(a2, 3) ^ a3; s[8 + c] = a0 ^ a1 ^ mul(a2, 2) ^ mul(a3, 3); s[12 + c] = mul(a0, 3) ^ a1 ^ a2 ^ mul(a3, 2); } }
    addRK(round);
  }
  const out = new Array(16); for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) out[c * 4 + r] = s[r * 4 + c];
  return out;
}
module.exports = { aesEncBlock, mul, keyExpand };

});
__define("aes_decrypt.js", function (module, exports, require, __filename, __dirname) {
'use strict';
// AES-128 decrypt with custom S-box (standard InvMixColumns/InvShiftRows)
const { keyExpand } = require('aes_encrypt.js');
function mul(a, b) { let r = 0; for (let i = 0; i < 8; i++) { if (b & 1) r ^= a; const hi = a & 0x80; a = (a << 1) & 0xff; if (hi) a ^= 0x1b; b >>= 1; } return r; }
function invSboxOf(sbox) { const inv = new Array(256); for (let i = 0; i < 256; i++) inv[sbox[i]] = i; return inv; }
function aesDecBlock(ct, key, sbox) {
  const inv = invSboxOf(sbox); const Nr = 10; const w = keyExpand(key, sbox);
  const s = new Array(16);
  for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) s[r * 4 + c] = ct[c * 4 + r];
  function addRK(round) { for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) s[r * 4 + c] ^= w[round * 4 + c][r]; }
  addRK(Nr);
  for (let round = Nr - 1; round >= 0; round--) {
    // InvShiftRows
    const t = s.slice(); for (let r = 1; r < 4; r++) for (let c = 0; c < 4; c++) s[r * 4 + c] = t[r * 4 + ((c - r + 4) % 4)];
    for (let i = 0; i < 16; i++) s[i] = inv[s[i]];
    addRK(round);
    if (round > 0) { const o = s.slice(); for (let c = 0; c < 4; c++) { const a0 = o[c], a1 = o[4 + c], a2 = o[8 + c], a3 = o[12 + c]; s[c] = mul(a0, 14) ^ mul(a1, 11) ^ mul(a2, 13) ^ mul(a3, 9); s[4 + c] = mul(a0, 9) ^ mul(a1, 14) ^ mul(a2, 11) ^ mul(a3, 13); s[8 + c] = mul(a0, 13) ^ mul(a1, 9) ^ mul(a2, 14) ^ mul(a3, 11); s[12 + c] = mul(a0, 11) ^ mul(a1, 13) ^ mul(a2, 9) ^ mul(a3, 14); } }
  }
  const out = new Array(16); for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) out[c * 4 + r] = s[r * 4 + c];
  return out;
}
module.exports = { aesDecBlock, invSboxOf };

});
__define("rap_crypto.js", function (module, exports, require, __filename, __dirname) {
/**
 * x-rap-param 纯算 —— 加密侧（crypto + 信封），零补环境。
 * 管线：payload → gzip(pure XHS) → alphabet36-random-pad → XOR(xorKey) → AES-128-ECB(自研S-box, 固定密钥) → body
 *       encR = AES-128-ECB(xorKey)
 *       信封 = header + nonce + encR + len + body + trailing(bodyLen)
 * 分组密码 = 标准 AES-128-ECB，仅 S-box 换成小红书自研表。
 * DEFLATE = deflate.js（简单贪婪LZ77 + 动态Huffman，字节对齐浏览器版本）。
 */
'use strict';
const crypto = require('crypto');
const zlib = require('zlib');
const { aesEncBlock } = require('aes_encrypt.js');
const { aesDecBlock } = require('aes_decrypt.js');
const { encodeGzip } = require('deflate.js');

const SBOX = [122,1,88,224,80,78,2,121,29,75,83,218,107,72,212,82,237,119,18,33,20,21,236,16,24,229,185,241,12,8,252,125,249,205,181,200,230,55,38,135,86,186,184,43,173,240,104,247,139,141,211,94,54,77,46,146,49,130,242,41,112,61,45,215,182,64,178,67,68,128,120,210,13,73,74,9,99,108,7,58,158,213,6,198,225,98,244,52,36,89,169,87,42,0,62,23,44,10,26,66,250,147,190,220,245,179,106,19,232,3,199,151,187,115,118,134,227,70,114,71,208,5,76,56,124,31,129,171,117,81,235,243,50,116,17,143,132,137,156,113,34,126,157,207,63,145,105,101,60,109,150,162,152,153,51,57,154,202,195,159,160,188,228,163,164,84,127,167,168,4,111,93,172,183,39,175,176,40,65,174,180,110,11,27,223,142,48,177,254,144,97,96,192,203,92,14,239,22,131,234,32,233,201,85,196,69,133,204,30,170,103,138,123,53,214,25,216,217,194,219,148,221,28,222,166,255,248,191,91,90,15,231,193,189,209,102,197,37,238,140,226,95,136,161,59,165,246,206,149,47,100,35,251,253,79,155];
const AES_KEY = [...Buffer.from('kqI1DTcwKX90ZtAy')];
const ALPHABET36 = '0123456789abcdefghijklmnopqrstuvwxyz';

function aesEcbEnc(data) { const o = []; for (let i = 0; i < data.length; i += 16) o.push(...aesEncBlock([...data.slice(i, i + 16)], AES_KEY.slice(), SBOX)); return Buffer.from(o); }
function aesEcbDec(data) { const o = []; for (let i = 0; i + 16 <= data.length; i += 16) o.push(...aesDecBlock([...data.slice(i, i + 16)], AES_KEY.slice(), SBOX)); return Buffer.from(o); }

// XHS 使用 alphabet36 随机 ASCII 填充到 16 字节边界 (NOT PKCS7)
function alpha36Pad(buf) {
  const n = 16 - (buf.length % 16);
  const padActual = n === 0 ? 16 : n;
  const rand = crypto.randomBytes(padActual);
  const padBytes = Buffer.from([...rand].map(b => ALPHABET36.charCodeAt(b % 36)));
  return Buffer.concat([buf, padBytes]);
}

function pkcs7(buf) { const n = 16 - (buf.length % 16); return Buffer.concat([buf, Buffer.alloc(n, n)]); }

// 加密：payload(Buffer) + xorKey(16B) → {encR, body, gzipLen}
// mtime 可选：想字节对齐浏览器需要提供 mtime 与 xorKey，padBytes（可选）覆盖随机填充
function encryptPayload(payload, xorKey, opts = {}) {
  let gz;
  try {
    gz = encodeGzip(payload, opts.mtime || 0);
  } catch (err) {
    // 大 payload（如 Creator 发布的 452B 指纹）会让自研 Huffman 树深超过 15
    // 触发 overflow；回退 Node zlib 裸 deflate（与浏览器同族的合法编码，
    // 服务端只做 AES-ECB + XOR + gunzip 校验，不逐字节比对 deflate）。
    const deflated = zlib.deflateRawSync(payload, { level: 9 });
    const mtime = opts.mtime || 0;
    gz = Buffer.alloc(10 + deflated.length + 8);
    gz[0] = 0x1f; gz[1] = 0x8b; gz[2] = 0x08; gz[3] = 0x00;
    gz.writeUInt32LE(mtime, 4);
    gz[8] = 0x00; gz[9] = 0x03;
    deflated.copy(gz, 10);
    gz.writeUInt32LE(zlib.crc32(payload), 10 + deflated.length);
    gz.writeUInt32LE(payload.length & 0xffffffff, 14 + deflated.length);
  }
  const gzipLen = gz.length;
  const padded = opts.padBytes
    ? Buffer.concat([gz, Buffer.from(opts.padBytes)])
    : alpha36Pad(gz);
  const xored = Buffer.from(padded.map((b, i) => b ^ xorKey[i % 16]));
  const body = aesEcbEnc(xored);
  const encR = aesEcbEnc(Buffer.from(xorKey));
  return { encR, body, gzipLen };
}

// 解密（验证用）— 通过尝试不同尾部长度找出 gzip 结束位置
function decryptBody(encR, body) {
  const xorKey = aesEcbDec(encR);
  const xored = aesEcbDec(body);
  const gz = Buffer.from(xored.map((b, i) => b ^ xorKey[i % 16]));
  for (let trim = 0; trim <= 16; trim++) { try { return zlib.gunzipSync(gz.slice(0, gz.length - trim)); } catch (e) {} }
  throw new Error('gunzip failed');
}

module.exports = { encryptPayload, decryptBody, aesEcbEnc, aesEcbDec, SBOX, AES_KEY, pkcs7, alpha36Pad };

});
__define("rap.js", function (module, exports, require, __filename, __dirname) {
/**
 * x-rap-param algorithm — zero VMP execution.
 *
 * Reversed 2026-07-15 from captured Sanji/Garp VMP browser evidence.
 *
 * Discoveries:
 *   - Xorkeyverifyvalue = xxHash32([mask_byte], seed=0)
 *   - RequestHash       = xxHash32(url_pathAndBeyond + jsonBody, seed=0)
 *   - Payload wire format: header(16B raw) + body(N B XOR-masked with 1B mask)
 *       header: [0:2]=0x03e8 op, [2:10]=8B ts BE, [10:12]=0x03e9 op, [12:16]=4B xorKeyVerify
 *       body: TLV opcodes 0x3ea..0x484, each XOR'd with mask
 *   - Random ASCII generation: bytes → alphabet36[b % 36]
 *   - AES key: "kqI1DTcwKX90ZtAy" (fixed)
 *   - Envelope: rap_crypto.js
 *
 * Fingerprint (~200B of behavior/detection data) is captured once per session
 * and reused (same approach as mns envConst/envFpTail).
 */
'use strict';
const crypto = require('crypto');
const { encryptPayload } = require('rap_crypto.js');

const ALPHABET36 = '0123456789abcdefghijklmnopqrstuvwxyz';

/** xxHash32 (32-bit variant of xxHash). Byte-exact vs reference. */
function xxHash32(buf, seed = 0) {
  const P1 = 0x9E3779B1 >>> 0, P2 = 0x85EBCA77 >>> 0, P3 = 0xC2B2AE3D >>> 0;
  const P4 = 0x27D4EB2F >>> 0, P5 = 0x165667B1 >>> 0;
  const M = Math.imul;
  const rotl = (x, n) => ((x << n) | (x >>> (32 - n))) >>> 0;
  let h, p = 0; const len = buf.length;
  if (len >= 16) {
    let v1 = (seed + P1 + P2) >>> 0, v2 = (seed + P2) >>> 0, v3 = seed >>> 0, v4 = (seed - P1) >>> 0;
    while (p + 16 <= len) {
      const l1 = (buf[p] | buf[p+1]<<8 | buf[p+2]<<16 | buf[p+3]<<24) >>> 0;
      const l2 = (buf[p+4] | buf[p+5]<<8 | buf[p+6]<<16 | buf[p+7]<<24) >>> 0;
      const l3 = (buf[p+8] | buf[p+9]<<8 | buf[p+10]<<16 | buf[p+11]<<24) >>> 0;
      const l4 = (buf[p+12] | buf[p+13]<<8 | buf[p+14]<<16 | buf[p+15]<<24) >>> 0;
      v1 = M(rotl((v1 + M(l1, P2)) >>> 0, 13), P1) >>> 0;
      v2 = M(rotl((v2 + M(l2, P2)) >>> 0, 13), P1) >>> 0;
      v3 = M(rotl((v3 + M(l3, P2)) >>> 0, 13), P1) >>> 0;
      v4 = M(rotl((v4 + M(l4, P2)) >>> 0, 13), P1) >>> 0;
      p += 16;
    }
    h = (rotl(v1, 1) + rotl(v2, 7) + rotl(v3, 12) + rotl(v4, 18)) >>> 0;
  } else h = (seed + P5) >>> 0;
  h = (h + len) >>> 0;
  while (p + 4 <= len) {
    const l = (buf[p] | buf[p+1]<<8 | buf[p+2]<<16 | buf[p+3]<<24) >>> 0;
    h = M(rotl((h + M(l, P3)) >>> 0, 17), P4) >>> 0; p += 4;
  }
  while (p < len) { h = M(rotl((h + M(buf[p], P5)) >>> 0, 11), P1) >>> 0; p++; }
  h ^= h >>> 15; h = M(h, P2) >>> 0; h ^= h >>> 13; h = M(h, P3) >>> 0; h ^= h >>> 16;
  return h >>> 0;
}

/** Map N raw random bytes to ASCII: alphabet36[b % 36] for each byte. */
function randomAscii(n) {
  const buf = crypto.randomBytes(n);
  let out = '';
  for (const b of buf) out += ALPHABET36[b % 36];
  return out;
}

/** Build a fresh 235B payload (no Uuid; the minimal common form).
 *  Returns Uint8Array of the plaintext payload (before gzip).
 *  fingerprint is a 201-byte Buffer captured from browser (or all-zeros for a bare minimum). */
function buildPayload({ ts, url, body, fingerprint, mask }) {
  // Layout for 235B payload (Uuid absent):
  //   [0:16]  header (raw)
  //   [16:38] Uuid section — for 235B we still need SOMETHING at [16:] starting with 0x03ea
  //   ... 219B body ...
  // Simpler: use the exact template we captured and only overwrite dynamic bytes.
  // Header: 03e8 <8B ts BE> 03e9 <4B xorKeyVerify>
  const p = new Uint8Array(fingerprint.length + 16);
  // Header
  p[0] = 0x03; p[1] = 0xe8;
  // 8B ts BE
  const tsHi = Math.floor(ts / 0x100000000);
  const tsLo = ts >>> 0;
  p[2] = (tsHi >>> 24) & 0xff; p[3] = (tsHi >>> 16) & 0xff; p[4] = (tsHi >>> 8) & 0xff; p[5] = tsHi & 0xff;
  p[6] = (tsLo >>> 24) & 0xff; p[7] = (tsLo >>> 16) & 0xff; p[8] = (tsLo >>> 8) & 0xff; p[9] = tsLo & 0xff;
  p[10] = 0x03; p[11] = 0xe9;
  // xorKeyVerify = xxHash32([mask])
  const verify = xxHash32(Buffer.from([mask]));
  p[12] = (verify >>> 24) & 0xff; p[13] = (verify >>> 16) & 0xff; p[14] = (verify >>> 8) & 0xff; p[15] = verify & 0xff;
  // Body: copy fingerprint template + write dynamic fields, then XOR with mask
  const bodyUnmasked = Buffer.from(fingerprint);
  // Fingerprint template must already have 0x03ea at [0] (Uuid) or 0x03eb at [0] (RequestHash).
  // Locate RequestHash slot (opcode 0x03eb) and write current URL+body hash.
  // Search for 03eb marker in template.
  let reqHashOff = -1;
  for (let i = 0; i < bodyUnmasked.length - 5; i++) {
    if (bodyUnmasked[i] === 0x03 && bodyUnmasked[i+1] === 0xeb) { reqHashOff = i + 2; break; }
  }
  if (reqHashOff >= 0) {
    const reqHash = xxHash32(Buffer.from(url + body));
    bodyUnmasked[reqHashOff]   = (reqHash >>> 24) & 0xff;
    bodyUnmasked[reqHashOff+1] = (reqHash >>> 16) & 0xff;
    bodyUnmasked[reqHashOff+2] = (reqHash >>> 8)  & 0xff;
    bodyUnmasked[reqHashOff+3] = reqHash & 0xff;
  }
  // XOR-mask the body
  for (let i = 0; i < bodyUnmasked.length; i++) p[16 + i] = bodyUnmasked[i] ^ mask;
  return p;
}

/** Full rap generator.
 *  Optional inputs for byte-exact reproduction: mtime, padBytes, bodyEncryTime.
 */
function buildRapPure({ api, data, fingerprint, mask, xorKey, ts, nonce, mtime, padBytes, bodyEncryTime, returnLayers }) {
  api = api || '/api/sns/web/v1/homefeed';
  data = typeof data === 'string' ? data : JSON.stringify(data || {});
  ts = ts || Date.now();
  // URL form used by the browser rap for RequestHash.
  // 2026-07-25 实证（Creator post_note 模板 03eb 槽反解）：
  // 浏览器对发布接口用**完整 https URL** 求 xxHash32(url+body)；
  // 传完整 URL 时按原样使用，传路径时退回协议相对形式（兼容 PC 旧调用）。
  let url;
  if (/^https?:\/\//.test(api)) url = api;
  else if (api.startsWith('//')) url = api;
  else url = '//edith.xiaohongshu.com' + api;

  // mask: a random byte mapped to alphabet36 ASCII (like '0'..'9','a'..'z')
  if (mask == null) mask = ALPHABET36.charCodeAt(crypto.randomBytes(1)[0] % 36);

  // xorKey: 16 ASCII chars from alphabet36
  if (!xorKey) xorKey = Buffer.from(randomAscii(16));
  else if (typeof xorKey === 'string') xorKey = Buffer.from(xorKey);

  // Envelope nonce: 4-6 ASCII chars — magic byte determines length. Accept override.
  let nonceLen;
  if (nonce != null) {
    if (typeof nonce === 'string') nonce = Buffer.from(nonce, 'ascii');
    nonceLen = nonce.length;
  } else {
    nonceLen = 4 + Math.floor(crypto.randomBytes(1)[0] / 86); // 4/5/6 roughly balanced
    nonce = Buffer.from(randomAscii(nonceLen), 'ascii');
  }

  // Build the plaintext payload (single canonical build; RequestHash uses url + data)
  const p2 = buildPayload({ ts, url, body: data, fingerprint, mask });

  // Encrypt via rap_crypto (DEFLATE + alphabet36 pad)
  const encryptStart = Date.now();
  const { encR, body: bodyEnc, gzipLen } = encryptPayload(Buffer.from(p2), Array.from(xorKey), { mtime: mtime || 0, padBytes });
  const encryptTimeMs = bodyEncryTime != null ? bodyEncryTime : Math.max(1, Date.now() - encryptStart);

  const B = bodyEnc.length;
  const env = Buffer.alloc(60 + nonceLen + B);
  env[0] = 0x07; env[1] = 0x24; env[2] = 0x01; env[3] = nonceLen;
  env.writeUInt32BE(1, 4);                              // ver (protocolCryptVersion)
  env.writeUInt32BE(20, 8);                             // keyLength (encR + encRlen)
  env.writeUInt32BE(B + 4, 12);                         // payloadLen (bodyLen + 4)
  // env[16:20] = chk16 (bodyhash) — computed below after body written
  env.writeUInt32BE(0x0000283d, 20);                    // Version (0x283d = fingerprint version)
  env.writeUInt32BE(encryptTimeMs, 24);                 // bodyEncryTime (elapsed ms)
  // env[28:36] = 0 (already zero from Buffer.alloc) — note: header prototype writes 512 at [28:32] but final envelope has 0
  nonce.copy(env, 36);
  Buffer.from(encR).copy(env, 36 + nonceLen);
  env.writeUInt32BE(16, 52 + nonceLen);
  Buffer.from(bodyEnc).copy(env, 56 + nonceLen);
  env.writeUInt32BE(gzipLen, 56 + nonceLen + B);  // trailing = actual gzip length (NOT encrypted body length)
  // Now compute chk16 = xxHash32(env[36:], seed=0) — hash of everything after the 36B header
  const chk16 = xxHash32(env.slice(36), 0);
  env.writeUInt32BE(chk16, 16);

  const rap = env.toString('base64');
  if (returnLayers) return { rap, plaintext: Buffer.from(p2), encR: Buffer.from(encR), body: Buffer.from(bodyEnc), envelope: env, mask, xorKey, nonce };
  return rap;
}

module.exports = { buildRapPure, xxHash32, randomAscii, ALPHABET36 };

});

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
