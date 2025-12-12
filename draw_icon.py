from PIL import Image, ImageDraw

def create_icon_foreground():
    size = 1024  # 高解析度來源
    # 建立一個完全透明的畫布 (RGBA)
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 在 11x11 的虛擬網格上設計 "M"
    grid_size = 11
    padding = 150  # 讓圖案在中心，周圍留白
    
    cell_size = (size - 2 * padding) / grid_size
    dot_radius = cell_size * 0.45

    # 定義 "M" 的點陣圖案 (Marquee)
    pattern = [
        [0,0,0,0,0,0,0,0,0,0,0],
        [0,1,0,0,0,0,0,0,0,1,0],
        [0,1,1,0,0,0,0,0,1,1,0],
        [0,1,0,1,0,0,0,1,0,1,0],
        [0,1,0,0,1,0,1,0,0,1,0],
        [0,1,0,0,0,1,0,0,0,1,0],
        [0,1,0,0,0,1,0,0,0,1,0],
        [0,1,0,0,0,0,0,0,0,1,0],
        [0,1,0,0,0,0,0,0,0,1,0],
        [0,0,0,0,0,0,0,0,0,0,0],
        [0,0,0,0,0,0,0,0,0,0,0],
    ]

    for r in range(grid_size):
        for c in range(grid_size):
            if pattern[r][c] == 1:
                cx = padding + c * cell_size + cell_size / 2
                cy = padding + r * cell_size + cell_size / 2
                # 畫白色的實心圓點
                draw.ellipse([cx - dot_radius, cy - dot_radius, cx + dot_radius, cy + dot_radius], fill=(255, 255, 255, 255))

    filename = 'icon_foreground.png'
    img.save(filename)
    return filename

file_path = create_icon_foreground()
print(f"Image saved to {file_path}")
