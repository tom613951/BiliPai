import os
from PIL import Image, ImageDraw

src_path = r"C:\Users\26503\.gemini\antigravity\brain\e8448cd6-4c32-4c20-b1e3-bcafaaea3d98\media__1780895129484.jpg"
res_base_dir = r"C:\Users\26503\Documents\antigravity\fearless-raman\BiliPai\app\src\main\res"

# Open source image and convert to RGBA
img = Image.open(src_path).convert("RGBA")

# Corner points for flood fill
corners = [(0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)]

# 1. Generate transparent version (for legacy icons)
img_trans = img.copy()
for x, y in corners:
    ImageDraw.floodfill(img_trans, (x, y), (0, 0, 0, 0), thresh=35)

# 2. Generate solid pink corner version (for adaptive backgrounds)
img_solid = img.copy()
for x, y in corners:
    ImageDraw.floodfill(img_solid, (x, y), (252, 139, 195, 255), thresh=35)

# Density mapping: (folder_suffix, legacy_size, adaptive_size)
densities = [
    ("mdpi", 48, 108),
    ("hdpi", 72, 162),
    ("xhdpi", 96, 216),
    ("xxhdpi", 144, 324),
    ("xxxhdpi", 192, 432)
]

legacy_names = [
    "ic_launcher.png",
    "ic_launcher_round.png",
    "ic_launcher_3d.png",
    "ic_launcher_3d_round.png",
    "ic_launcher_bilipai.png",
    "ic_launcher_bilipai_round.png",
    "ic_launcher_bilipai_pink.png",
    "ic_launcher_bilipai_pink_round.png",
    "ic_launcher_yuki.png",
    "ic_launcher_yuki_round.png"
]

adaptive_bg_names = [
    "ic_launcher_3d_foreground.png",
    "ic_launcher_yuki_foreground.png",
    "ic_launcher_bilipai_foreground.png",
    "ic_launcher_bilipai_pink_foreground.png"
]

for suffix, l_size, a_size in densities:
    folder = os.path.join(res_base_dir, f"mipmap-{suffix}")
    if not os.path.exists(folder):
        os.makedirs(folder)
    
    # Resize transparent version for legacy icons
    resized_trans = img_trans.resize((l_size, l_size), Image.Resampling.LANCZOS)
    for name in legacy_names:
        dest_path = os.path.join(folder, name)
        resized_trans.save(dest_path, "PNG")
        print(f"Saved legacy transparent icon to {dest_path}")
        
    # Resize solid pink version for adaptive backgrounds
    resized_solid = img_solid.resize((a_size, a_size), Image.Resampling.LANCZOS)
    for name in adaptive_bg_names:
        dest_path = os.path.join(folder, name)
        resized_solid.convert("RGB").save(dest_path, "PNG")
        print(f"Saved adaptive background to {dest_path}")

print("Icon generation complete!")
