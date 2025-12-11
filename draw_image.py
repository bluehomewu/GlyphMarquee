from PIL import Image, ImageDraw

def create_centered_original_style_preview():
    size = 512
    # 建立透明背景
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # 建立內容圖層
    content_layer = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(content_layer)

    # 1. 畫出圓形黑色底
    margin = 2
    draw.ellipse((margin, margin, size-margin, size-margin), fill=(15, 15, 15, 255))

    # 2. 繪製點陣 (15x15 網格)
    rows, cols = 15, 15
    padding = 60 
    
    cell_w = (size - 2 * padding) / cols
    cell_h = (size - 2 * padding) / rows
    dot_radius = cell_w * 0.35

    # 3. 定義圖案 (原本的風格，但整體向左平移 1 格)
    # 原本最長的一排是 index 3~13 (中心在 8)，現在改為 2~12 (中心在 7)
    pattern = [
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,1,0,0,0,1,0,0,0,0,0], # Row 4: 移至 5, 9
        [0,0,0,0,1,1,0,0,0,1,1,0,0,0,0], # Row 5: 移至 4,5, 9,10
        [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0], # Row 6: 移至 3~11
        [0,0,1,1,1,1,1,1,1,1,1,1,1,0,0], # Row 7 (中心): 移至 2~12 (長度11，中心為7) -> 完美置中
        [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0], # Row 8: 移至 3~11
        [0,0,0,0,1,1,0,0,0,1,1,0,0,0,0], # Row 9: 移至 4,5, 9,10
        [0,0,0,0,0,1,0,0,0,1,0,0,0,0,0], # Row 10: 移至 5, 9
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    ]

    for r in range(rows):
        for c in range(cols):
            cx = padding + c * cell_w + cell_w / 2
            cy = padding + r * cell_h + cell_h / 2
            
            # 圓形範圍檢查
            dist_center = ((cx - size/2)**2 + (cy - size/2)**2)**0.5
            if dist_center > (size/2 - margin):
                continue
            
            # 取得圖案點
            is_lit = pattern[r][c] == 1 if r < len(pattern) and c < len(pattern[0]) else False
            
            if is_lit:
                # 亮燈：純白
                draw.ellipse([cx - dot_radius, cy - dot_radius, cx + dot_radius, cy + dot_radius], fill=(255, 255, 255, 255))
            else:
                # 滅燈：深灰色
                draw.ellipse([cx - dot_radius * 0.8, cy - dot_radius * 0.8, cx + dot_radius * 0.8, cy + dot_radius * 0.8], fill=(45, 45, 45, 255))

    # 4. 圓形裁切
    mask = Image.new('L', (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.ellipse((0, 0, size, size), fill=255)

    final_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    final_img.paste(content_layer, (0, 0), mask=mask)

    filename = 'toy_preview.png'
    final_img.save(filename)
    return filename

file_path = create_centered_original_style_preview()
print(f"Image saved to {file_path}")
