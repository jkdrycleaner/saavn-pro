const fs = require('fs');
const zlib = require('zlib');

function crc32(buf) {
  let table = new Int32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) {
      c = ((c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1));
    }
    table[i] = c;
  }
  let crc = -1;
  for (let i = 0; i < buf.length; i++) {
    crc = (crc >>> 8) ^ table[(crc ^ buf[i]) & 0xFF];
  }
  return (crc ^ (-1)) >>> 0;
}

function makePng(filename, width, height) {
  const barXs = [0.25, 0.38, 0.50, 0.62, 0.75].map(f => Math.floor(width * f));
  const barHeights = [0.35, 0.65, 0.90, 0.60, 0.40];
  const barW = Math.max(4, Math.floor(width * 0.07));

  let raw = [];
  for (let y = 0; y < height; y++) {
    raw.push(0); // filter type 0
    for (let x = 0; x < width; x++) {
      let inBar = false;
      for (let i = 0; i < barXs.length; i++) {
        let bx = barXs[i];
        let bh = Math.floor(height * barHeights[i] * 0.65);
        let yTop = Math.floor((height - bh) / 2);
        let yBottom = yTop + bh;
        if (x >= bx - barW / 2 && x <= bx + barW / 2 && y >= yTop && y <= yBottom) {
          inBar = true;
          break;
        }
      }
      if (inBar) {
        raw.push(43, 197, 180, 255); // #2BC5B4 teal
      } else {
        raw.push(18, 18, 18, 255); // #121212 dark
      }
    }
  }

  const rawBuffer = Buffer.from(raw);
  const idatData = zlib.deflateSync(rawBuffer);

  const pngHeader = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

  // IHDR
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr.writeUInt8(8, 8); // 8-bit
  ihdr.writeUInt8(6, 9); // RGBA
  ihdr.writeUInt8(0, 10);
  ihdr.writeUInt8(0, 11);
  ihdr.writeUInt8(0, 12);
  const ihdrChunk = makeChunk('IHDR', ihdr);

  // IDAT
  const idatChunk = makeChunk('IDAT', idatData);

  // IEND
  const iendChunk = makeChunk('IEND', Buffer.alloc(0));

  const total = Buffer.concat([pngHeader, ihdrChunk, idatChunk, iendChunk]);
  fs.writeFileSync(filename, total);
}

function makeChunk(type, data) {
  const typeBuf = Buffer.from(type, 'ascii');
  const lenBuf = Buffer.alloc(4);
  lenBuf.writeUInt32BE(data.length, 0);

  const toCrc = Buffer.concat([typeBuf, data]);
  const crcVal = crc32(toCrc);
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crcVal, 0);

  return Buffer.concat([lenBuf, typeBuf, data, crcBuf]);
}

makePng('icon-192.png', 192, 192);
makePng('icon-512.png', 512, 512);
console.log('Icons created with Node.js successfully.');
