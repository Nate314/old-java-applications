// A QR code maker, for the home page's Your records (sending saves to a phone): byte mode, error correction level L
// (the fewest modules for a given text, so the easiest to scan from a screen), versions 1 to 40, following
// ISO/IEC 18004 the way Project Nayuki's QR Code generator lays it out. qr(text) is null when the text is too long
// for any version; QrCode.tsx draws one.
export interface QrCode { size: number; version: number; level: string; dark(x: number, y: number): boolean }

// Per level (only L here), per version 1..40: error correction codewords per block, and the number of blocks.
const ECC: Record<string, number[]> = {
  L: [7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30]
};
const BLOCKS: Record<string, number[]> = {
  L: [1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25]
};
const FORMAT: Record<string, number> = { L: 1 };

function rawModules(ver: number): number {
  var n = (16 * ver + 128) * ver + 64;
  if (ver >= 2) {
    var align = Math.floor(ver / 7) + 2;
    n -= (25 * align - 10) * align - 55;
    if (ver >= 7) n -= 36;
  }
  return n;
}
function dataCodewords(ver: number, ecl: string): number {
  return Math.floor(rawModules(ver) / 8) - ECC[ecl][ver - 1] * BLOCKS[ecl][ver - 1];
}

// GF(256) with the QR polynomial 0x11D.
function mul(x: number, y: number): number {
  var z = 0;
  for (var i = 7; i >= 0; i--) {
    z = (z << 1) ^ ((z >>> 7) * 0x11D);
    z ^= ((y >>> i) & 1) * x;
  }
  return z;
}
function divisor(degree: number): number[] {
  var result: number[] = [];
  for (var i = 0; i < degree - 1; i++) result.push(0);
  result.push(1);
  var root = 1;
  for (i = 0; i < degree; i++) {
    for (var j = 0; j < result.length; j++) {
      result[j] = mul(result[j], root);
      if (j + 1 < result.length) result[j] ^= result[j + 1];
    }
    root = mul(root, 0x02);
  }
  return result;
}
function remainder(data: number[], div: number[]): number[] {
  var result = div.map(function () { return 0; });
  data.forEach(function (b) {
    var factor = b ^ (result.shift() as number);
    result.push(0);
    div.forEach(function (coef, i) { result[i] ^= mul(coef, factor); });
  });
  return result;
}

function utf8(text: string): number[] {
  return Array.from(new TextEncoder().encode(text));
}

export function qr(text: string): QrCode | null {
  var bytes = utf8(text);
  var ver = 0, ecl = 'L';
  for (var v = 1; v <= 40 && !ver; v++) {
    if (4 + (v <= 9 ? 8 : 16) + bytes.length * 8 <= dataCodewords(v, ecl) * 8) ver = v;
  }
  if (!ver) return null;

  // Data bits: byte mode, count, bytes, terminator, then padding.
  var bits: number[] = [];
  function put(value: number, len: number) { for (var i = len - 1; i >= 0; i--) bits.push((value >>> i) & 1); }
  put(4, 4);
  put(bytes.length, ver <= 9 ? 8 : 16);
  bytes.forEach(function (b) { put(b, 8); });
  var capacity = dataCodewords(ver, ecl) * 8;
  put(0, Math.min(4, capacity - bits.length));
  put(0, (8 - bits.length % 8) % 8);
  for (var pad = 0xEC; bits.length < capacity; pad ^= 0xEC ^ 0x11) put(pad, 8);
  var data: number[] = [];
  for (var i = 0; i < bits.length; i += 8) {
    var byte = 0;
    for (var k = 0; k < 8; k++) byte = (byte << 1) | bits[i + k];
    data.push(byte);
  }

  // Split into blocks, add each block's error correction, interleave.
  var numBlocks = BLOCKS[ecl][ver - 1], eccLen = ECC[ecl][ver - 1];
  var raw = Math.floor(rawModules(ver) / 8);
  var shortBlocks = numBlocks - raw % numBlocks, shortLen = Math.floor(raw / numBlocks);
  var div = divisor(eccLen), blocks: number[][] = [];
  for (i = 0, k = 0; i < numBlocks; i++) {
    var dat = data.slice(k, k + shortLen - eccLen + (i < shortBlocks ? 0 : 1));
    k += dat.length;
    var ecc = remainder(dat, div);
    if (i < shortBlocks) dat.push(0);
    blocks.push(dat.concat(ecc));
  }
  var codewords: number[] = [];
  for (i = 0; i < blocks[0].length; i++) {
    for (var j = 0; j < blocks.length; j++) {
      if (i !== shortLen - eccLen || j >= shortBlocks) codewords.push(blocks[j][i]);
    }
  }

  // The grid: function patterns first.
  var size = ver * 4 + 17;
  var mods: boolean[][] = [], fn: boolean[][] = [];
  for (i = 0; i < size; i++) { mods.push(new Array(size).fill(false)); fn.push(new Array(size).fill(false)); }
  function set(x: number, y: number, dark: boolean) { mods[y][x] = dark; fn[y][x] = true; }
  for (i = 0; i < size; i++) { set(6, i, i % 2 === 0); set(i, 6, i % 2 === 0); }
  [[3, 3], [size - 4, 3], [3, size - 4]].forEach(function (c) {
    for (var dy = -4; dy <= 4; dy++) {
      for (var dx = -4; dx <= 4; dx++) {
        var d = Math.max(Math.abs(dx), Math.abs(dy)), x = c[0] + dx, y = c[1] + dy;
        if (x >= 0 && x < size && y >= 0 && y < size) set(x, y, d !== 2 && d !== 4);
      }
    }
  });
  var align: number[] = [];
  if (ver > 1) {
    var count = Math.floor(ver / 7) + 2;
    var step = ver === 32 ? 26 : Math.ceil((ver * 4 + 4) / (count * 2 - 2)) * 2;
    align = [6];
    for (var pos = size - 7; align.length < count; pos -= step) align.splice(1, 0, pos);
  }
  align.forEach(function (ax, ai) {
    align.forEach(function (ay, aj) {
      var last = align.length - 1;
      if ((ai === 0 && aj === 0) || (ai === 0 && aj === last) || (ai === last && aj === 0)) return;
      for (var dy = -2; dy <= 2; dy++) {
        for (var dx = -2; dx <= 2; dx++) set(ax + dx, ay + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
      }
    });
  });
  function drawFormat(mask: number) {
    var d = FORMAT[ecl] << 3 | mask, rem = d;
    for (var n = 0; n < 10; n++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
    var f = (d << 10 | rem) ^ 0x5412;
    function bit(n: number) { return ((f >>> n) & 1) !== 0; }
    for (n = 0; n <= 5; n++) set(8, n, bit(n));
    set(8, 7, bit(6));
    set(8, 8, bit(7));
    set(7, 8, bit(8));
    for (n = 9; n < 15; n++) set(14 - n, 8, bit(n));
    for (n = 0; n < 8; n++) set(size - 1 - n, 8, bit(n));
    for (n = 8; n < 15; n++) set(8, size - 15 + n, bit(n));
    set(8, size - 8, true);
  }
  drawFormat(0);
  if (ver >= 7) {
    var rem = ver;
    for (i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
    var vbits = ver << 12 | rem;
    for (i = 0; i < 18; i++) {
      var b = ((vbits >>> i) & 1) !== 0, a = size - 11 + i % 3, c = Math.floor(i / 3);
      set(a, c, b);
      set(c, a, b);
    }
  }

  // The codewords, in the zigzag of two-module columns from the bottom right.
  var n = 0;
  for (var right = size - 1; right >= 1; right -= 2) {
    if (right === 6) right = 5;
    for (var vert = 0; vert < size; vert++) {
      for (j = 0; j < 2; j++) {
        var x = right - j, up = ((right + 1) & 2) === 0, y = up ? size - 1 - vert : vert;
        if (!fn[y][x] && n < codewords.length * 8) {
          mods[y][x] = ((codewords[n >>> 3] >>> (7 - (n & 7))) & 1) !== 0;
          n++;
        }
      }
    }
  }

  var MASKS = [
    function (x: number, y: number) { return (x + y) % 2 === 0; },
    function (_x: number, y: number) { return y % 2 === 0; },
    function (x: number) { return x % 3 === 0; },
    function (x: number, y: number) { return (x + y) % 3 === 0; },
    function (x: number, y: number) { return (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0; },
    function (x: number, y: number) { return x * y % 2 + x * y % 3 === 0; },
    function (x: number, y: number) { return (x * y % 2 + x * y % 3) % 2 === 0; },
    function (x: number, y: number) { return ((x + y) % 2 + x * y % 3) % 2 === 0; }
  ];
  function applyMask(m: number) {
    for (var y = 0; y < size; y++) {
      for (var x = 0; x < size; x++) if (!fn[y][x] && MASKS[m](x, y)) mods[y][x] = !mods[y][x];
    }
  }
  // A simplified penalty (runs of five or more, 2 x 2 blocks, dark/light balance) picks the mask.
  function penalty() {
    var p = 0, dark = 0, x: number, y: number, run: number, prev: boolean | null;
    for (var pass = 0; pass < 2; pass++) {
      for (var a = 0; a < size; a++) {
        run = 0; prev = null;
        for (var b = 0; b < size; b++) {
          var v = pass ? mods[b][a] : mods[a][b];
          if (v === prev) { run++; if (run === 5) p += 3; else if (run > 5) p++; } else { run = 1; prev = v; }
        }
      }
    }
    for (y = 0; y < size; y++) {
      for (x = 0; x < size; x++) {
        if (mods[y][x]) dark++;
        if (x < size - 1 && y < size - 1) {
          var c = mods[y][x];
          if (c === mods[y][x + 1] && c === mods[y + 1][x] && c === mods[y + 1][x + 1]) p += 3;
        }
      }
    }
    return p + Math.floor(Math.abs(dark * 20 - size * size * 10) / (size * size)) * 10;
  }
  var best = 0, lowest = Infinity;
  for (var m = 0; m < 8; m++) {
    applyMask(m);
    drawFormat(m);
    var score = penalty();
    if (score < lowest) { lowest = score; best = m; }
    applyMask(m);
  }
  applyMask(best);
  drawFormat(best);
  return { size: size, version: ver, level: ecl, dark: function (x: number, y: number) { return mods[y][x]; } };
}
