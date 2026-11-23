import struct
import zlib

def make_png(filename, width, height, color_bg=(18, 18, 18), color_bar=(43, 197, 180)):
    # Create raw pixel buffer (RGBA)
    pixels = []
    # 5 bars
    bar_xs = [int(width * frac) for frac in [0.25, 0.38, 0.50, 0.62, 0.75]]
    bar_heights = [0.35, 0.65, 0.85, 0.55, 0.40]
    bar_w = max(4, int(width * 0.06))

    for y in range(height):
        row = [0] # filter byte
        for x in range(width):
            # Check if inside any bar
            in_bar = False
            for bx, bh in zip(bar_xs, bar_heights):
                bar_h_pixels = int(height * bh * 0.65)
                y_top = (height - bar_h_pixels) // 2
                y_bottom = (height + bar_h_pixels) // 2
                if (bx - bar_w // 2 <= x <= bx + bar_w // 2) and (y_top <= y <= y_bottom):
                    in_bar = True
                    break
            
            if in_bar:
                row.extend([color_bar[0], color_bar[1], color_bar[2], 255])
            else:
                row.extend([color_bg[0], color_bg[1], color_bg[2], 255])
        pixels.extend(row)

    raw_data = bytes(pixels)
    compressor = zlib.compressobj()
    idat_data = compressor.compress(raw_data) + compressor.flush()

    png = b'\x89PNG\r\n\x1a\n'
    
    # IHDR
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    png += struct.pack('>I', len(ihdr_data)) + b'IHDR' + ihdr_data + struct.pack('>I', zlib.crc32(b'IHDR' + ihdr_data))
    
    # IDAT
    png += struct.pack('>I', len(idat_data)) + b'IDAT' + idat_data + struct.pack('>I', zlib.crc32(b'IDAT' + idat_data))
    
    # IEND
    png += struct.pack('>I', 0) + b'IEND' + struct.pack('>I', zlib.crc32(b'IEND'))

    with open(filename, 'wb') as f:
        f.write(png)

make_png('icon-192.png', 192, 192)
make_png('icon-512.png', 512, 512)
print("Icons created successfully.")
