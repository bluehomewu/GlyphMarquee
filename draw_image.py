from PIL import Image, ImageDraw

def create_final_original_style_hardware_preview():
    size = 512
    # 建立透明背景
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # 建立內容圖層
    content_layer = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(content_layer)

    # 1. 畫出圓形黑色底
    draw.ellipse((0, 0, size, size), fill=(10, 10, 10, 255))

    # 2. 設定硬體規格: 25x25 Grid
    hw_rows, hw_cols = 25, 25
    padding = 20 
    cell_w = (size - 2 * padding) / hw_cols
    cell_h = (size - 2 * padding) / hw_rows
    # 調整燈珠大小：因為圖案比較細緻，燈珠稍微縮小一點點以呈現間隙感，會更像真實的 Nothing Phone
    dot_radius = cell_w * 0.38 

    # 硬體中心
    center_idx = 12 
    radius_threshold = 12.5

    # 3. 定義 "原本風格" 的雙箭頭圖案 (15x15)
    # 這就是你覺得比較好看的那個版本 (Row 7 滿版置中)
    pattern_15x15 = [
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,1,0,0,0,1,0,0,0,0,0], # Row 4
        [0,0,0,0,1,1,0,0,0,1,1,0,0,0,0], # Row 5
        [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0], # Row 6
        [0,0,1,1,1,1,1,1,1,1,1,1,1,0,0], # Row 7 (中心)
        [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0], # Row 8
        [0,0,0,0,1,1,0,0,0,1,1,0,0,0,0], # Row 9
        [0,0,0,0,0,1,0,0,0,1,0,0,0,0,0], # Row 10
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    ]

    # 計算偏移量：將 15x15 放在 25x25 的正中間
    # (25 - 15) / 2 = 5
    offset = 5

    for r in range(hw_rows):
        for c in range(hw_cols):
            # 計算物理座標
            cx = padding + c * cell_w + cell_w / 2
            cy = padding + r * cell_h + cell_h / 2
            
            # 判斷是否在硬體圓形有效範圍內
            dist_grid = ((r - center_idx)**2 + (c - center_idx)**2)**0.5
            
            if dist_grid > radius_threshold:
                continue 

            # 判斷亮燈
            is_lit = False
            # 轉換到 pattern 座標
            pr = r - offset
            pc = c - offset
            
            if 0 <= pr < 15 and 0 <= pc < 15:
                if pattern_15x15[pr][pc] == 1:
                    is_lit = True

            if is_lit:
                # 亮燈：純白
                draw.ellipse([cx - dot_radius, cy - dot_radius, cx + dot_radius, cy + dot_radius], fill=(255, 255, 255, 255))
            else:
                # 滅燈：深灰色 (模擬真實存在的燈珠)
                draw.ellipse([cx - dot_radius * 0.85, cy - dot_radius * 0.85, cx + dot_radius * 0.85, cy + dot_radius * 0.85], fill=(35, 35, 35, 255))

    # 4. 圓形裁切
    mask = Image.new('L', (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.ellipse((0, 0, size, size), fill=255)

    final_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    final_img.paste(content_layer, (0, 0), mask=mask)

    filename = 'toy_preview.png'
    final_img.save(filename)
    return filename

file_path = create_final_original_style_hardware_preview()
print(f"Image saved to {file_path}")
