#!/usr/bin/env python3
"""
Generate professional GUI textures for all machine blocks.
Standard Minecraft GUI size: 176x166 pixels
"""

from PIL import Image, ImageDraw

GUI_PATH = "src/main/resources/assets/advancedsorter/textures/gui"

# Colors
BG_COLOR = (198, 198, 198)  # Standard MC gray
BORDER_DARK = (85, 85, 85)
BORDER_LIGHT = (255, 255, 255)
SLOT_BG = (139, 139, 139)
SLOT_BORDER_DARK = (55, 55, 55)
SLOT_BORDER_LIGHT = (255, 255, 255, 128)

# Energy bar colors
ENERGY_BG = (40, 40, 40)
ENERGY_FILL = (200, 50, 50)  # Red for RF

# Progress arrow colors
ARROW_BG = (180, 180, 180)
ARROW_FILL = (255, 255, 255)


def draw_slot(draw, x, y, size=18):
    """Draw a standard inventory slot."""
    # Dark border (top and left)
    draw.rectangle([x, y, x + size - 1, y], fill=SLOT_BORDER_DARK)
    draw.rectangle([x, y, x, y + size - 1], fill=SLOT_BORDER_DARK)
    # Light border (bottom and right)
    draw.rectangle([x, y + size - 1, x + size - 1, y + size - 1], fill=BORDER_LIGHT)
    draw.rectangle([x + size - 1, y, x + size - 1, y + size - 1], fill=BORDER_LIGHT)
    # Slot background
    draw.rectangle([x + 1, y + 1, x + size - 2, y + size - 2], fill=SLOT_BG)


def draw_energy_bar_bg(draw, x, y, width=14, height=50):
    """Draw energy bar background."""
    # Border
    draw.rectangle([x, y, x + width - 1, y + height - 1], outline=SLOT_BORDER_DARK)
    # Background
    draw.rectangle([x + 1, y + 1, x + width - 2, y + height - 2], fill=ENERGY_BG)
    # Grid lines for visual effect
    for i in range(5):
        ly = y + 10 + i * 10
        draw.line([x + 2, ly, x + width - 3, ly], fill=(60, 60, 60))


def draw_progress_arrow_bg(draw, x, y):
    """Draw progress arrow background (24x17)."""
    # Arrow shape pointing right
    # Simple arrow: rectangle with triangle
    draw.rectangle([x, y + 4, x + 16, y + 12], fill=ARROW_BG)
    # Triangle tip
    for i in range(8):
        draw.line([x + 16 + i, y + 4 + i, x + 16 + i, y + 12 - i], fill=ARROW_BG)


def draw_tank_bg(draw, x, y, width=16, height=50):
    """Draw fluid/gas tank background."""
    draw.rectangle([x, y, x + width - 1, y + height - 1], outline=SLOT_BORDER_DARK)
    draw.rectangle([x + 1, y + 1, x + width - 2, y + height - 2], fill=(60, 80, 100))


def draw_player_inventory(draw, start_y=84):
    """Draw player inventory slots (3 rows + hotbar)."""
    # Main inventory (3 rows of 9)
    for row in range(3):
        for col in range(9):
            draw_slot(draw, 7 + col * 18, start_y + row * 18)

    # Hotbar (1 row of 9, with gap)
    hotbar_y = start_y + 58
    for col in range(9):
        draw_slot(draw, 7 + col * 18, hotbar_y)


def draw_gui_border(draw, width=176, height=166):
    """Draw the standard GUI border."""
    # Outer light border (top and left)
    draw.rectangle([0, 0, width - 1, 0], fill=BORDER_LIGHT)
    draw.rectangle([0, 0, 0, height - 1], fill=BORDER_LIGHT)
    draw.rectangle([1, 1, width - 2, 1], fill=BORDER_LIGHT)
    draw.rectangle([1, 1, 1, height - 2], fill=BORDER_LIGHT)

    # Outer dark border (bottom and right)
    draw.rectangle([0, height - 1, width - 1, height - 1], fill=BORDER_DARK)
    draw.rectangle([width - 1, 0, width - 1, height - 1], fill=BORDER_DARK)
    draw.rectangle([1, height - 2, width - 2, height - 2], fill=BORDER_DARK)
    draw.rectangle([width - 2, 1, width - 2, height - 2], fill=BORDER_DARK)


def create_base_gui():
    """Create base GUI with border and player inventory."""
    img = Image.new('RGBA', (176, 166), BG_COLOR)
    draw = ImageDraw.Draw(img)
    draw_gui_border(draw)
    draw_player_inventory(draw)
    return img, draw


def create_crusher_gui():
    """Crusher: Cobblestone -> Sand"""
    img, draw = create_base_gui()

    # Title area (leave space for text drawn by code)

    # Energy bar (left side)
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Input slot
    draw_slot(draw, 55, 34)

    # Progress arrow
    draw_progress_arrow_bg(draw, 79, 35)

    # Output slot
    draw_slot(draw, 115, 34)

    # Energy bar overlay (for fill) - stored at 176, 0
    # Draw energy fill pattern at 176, 0 (14x52)
    for y in range(52):
        r = int(200 - y * 1.5)
        g = int(50 + y * 0.5)
        draw.line([176, y, 189, y], fill=(max(50, r), g, 50))

    # Progress arrow fill at 176, 52 (24x17)
    draw.rectangle([176, 52, 199, 68], fill=ARROW_BG)
    for i in range(8):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=ARROW_BG)
    # Make it white/bright for "filled" state
    draw.rectangle([176, 52, 192, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    img.save(f"{GUI_PATH}/crusher.png")
    print("Created: crusher.png")


def create_crucible_gui():
    """Crucible: Cobblestone -> Lava (with fluid tank)"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Input slot
    draw_slot(draw, 55, 34)

    # Progress arrow
    draw_progress_arrow_bg(draw, 79, 35)

    # Lava tank (output)
    draw_tank_bg(draw, 116, 18, 18, 52)
    # Orange tint for lava
    draw.rectangle([117, 19, 132, 68], fill=(80, 40, 20))

    # Overlays
    for y in range(52):
        r = int(200 - y * 1.5)
        g = int(50 + y * 0.5)
        draw.line([176, y, 189, y], fill=(max(50, r), g, 50))

    draw.rectangle([176, 52, 199, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    # Lava fill overlay at 176, 70 (16x52)
    for y in range(52):
        orange = int(255 - y * 2)
        draw.line([176, 70 + y, 191, 70 + y], fill=(255, max(50, orange), 0))

    img.save(f"{GUI_PATH}/crucible.png")
    print("Created: crucible.png")


def create_washer_gui():
    """Washer: uses water, has fluid tanks"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Water tank (input fluid)
    draw_tank_bg(draw, 26, 18, 16, 52)
    draw.rectangle([27, 19, 40, 68], fill=(40, 60, 100))

    # Input slot
    draw_slot(draw, 55, 34)

    # Progress arrow
    draw_progress_arrow_bg(draw, 79, 35)

    # Output slot
    draw_slot(draw, 115, 34)

    # Waste tank (output fluid)
    draw_tank_bg(draw, 140, 18, 16, 52)
    draw.rectangle([141, 19, 154, 68], fill=(60, 50, 40))

    # Overlays
    for y in range(52):
        r = int(200 - y * 1.5)
        g = int(50 + y * 0.5)
        draw.line([176, y, 189, y], fill=(max(50, r), g, 50))

    draw.rectangle([176, 52, 199, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    # Water fill at 176, 70
    for y in range(52):
        draw.line([176, 70 + y, 191, 70 + y], fill=(50, 100, 200))

    img.save(f"{GUI_PATH}/washer.png")
    print("Created: washer.png")


def create_cobble_generator_gui():
    """Cobble Generator: water + lava + energy = cobblestone"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Water tank
    draw_tank_bg(draw, 30, 18, 16, 52)
    draw.rectangle([31, 19, 44, 68], fill=(40, 60, 100))

    # Lava tank
    draw_tank_bg(draw, 50, 18, 16, 52)
    draw.rectangle([51, 19, 64, 68], fill=(80, 40, 20))

    # Progress arrow
    draw_progress_arrow_bg(draw, 79, 35)

    # Output slot
    draw_slot(draw, 115, 34)

    # Overlays
    for y in range(52):
        r = int(200 - y * 1.5)
        draw.line([176, y, 189, y], fill=(max(50, r), 50, 50))

    draw.rectangle([176, 52, 199, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    # Water/Lava fills
    for y in range(52):
        draw.line([176, 70 + y, 191, 70 + y], fill=(50, 100, 200))
    for y in range(52):
        draw.line([192, 70 + y, 207, 70 + y], fill=(255, 100, 0))

    img.save(f"{GUI_PATH}/cobble_generator.png")
    print("Created: cobble_generator.png")


def create_obsidian_generator_gui():
    """Obsidian Generator: water + lava + energy = obsidian (slower)"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Water tank
    draw_tank_bg(draw, 30, 18, 16, 52)
    draw.rectangle([31, 19, 44, 68], fill=(40, 60, 100))

    # Lava tank
    draw_tank_bg(draw, 50, 18, 16, 52)
    draw.rectangle([51, 19, 64, 68], fill=(80, 40, 20))

    # Progress arrow
    draw_progress_arrow_bg(draw, 79, 35)

    # Output slot
    draw_slot(draw, 115, 34)

    # Overlays (same as cobble)
    for y in range(52):
        r = int(200 - y * 1.5)
        draw.line([176, y, 189, y], fill=(max(50, r), 50, 50))

    draw.rectangle([176, 52, 199, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    for y in range(52):
        draw.line([176, 70 + y, 191, 70 + y], fill=(50, 100, 200))
    for y in range(52):
        draw.line([192, 70 + y, 207, 70 + y], fill=(255, 100, 0))

    img.save(f"{GUI_PATH}/obsidian_generator.png")
    print("Created: obsidian_generator.png")


def create_deconstructor_gui():
    """Deconstructor: breaks items into components"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Input slot (center-left)
    draw_slot(draw, 55, 34)

    # Progress arrow
    draw_progress_arrow_bg(draw, 79, 35)

    # Output slots (3x3 grid)
    for row in range(3):
        for col in range(3):
            draw_slot(draw, 107 + col * 18, 16 + row * 18)

    # Overlays
    for y in range(52):
        r = int(200 - y * 1.5)
        draw.line([176, y, 189, y], fill=(max(50, r), 50, 50))

    draw.rectangle([176, 52, 199, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    img.save(f"{GUI_PATH}/deconstructor.png")
    print("Created: deconstructor.png")


def create_auto_repairer_gui():
    """Auto Repairer: repairs tools using energy"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Input slot (tool to repair)
    draw_slot(draw, 55, 34)

    # Progress arrow (anvil style)
    draw_progress_arrow_bg(draw, 79, 35)

    # Output slot
    draw_slot(draw, 115, 34)

    # Material slot (for repair material)
    draw_slot(draw, 55, 55)

    # Overlays
    for y in range(52):
        r = int(200 - y * 1.5)
        draw.line([176, y, 189, y], fill=(max(50, r), 50, 50))

    draw.rectangle([176, 52, 199, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    img.save(f"{GUI_PATH}/auto_repairer.png")
    print("Created: auto_repairer.png")


def create_ammo_press_gui():
    """Ammo Press: creates ammunition"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Input slots (2 for materials)
    draw_slot(draw, 44, 25)
    draw_slot(draw, 44, 45)

    # Progress arrow
    draw_progress_arrow_bg(draw, 70, 35)

    # Output slot
    draw_slot(draw, 100, 35)

    # Template/recipe slot
    draw_slot(draw, 130, 35)

    # Overlays
    for y in range(52):
        r = int(200 - y * 1.5)
        draw.line([176, y, 189, y], fill=(max(50, r), 50, 50))

    draw.rectangle([176, 52, 199, 68], fill=(255, 255, 255))
    for i in range(7):
        draw.line([192 + i, 56 + i, 192 + i, 64 - i], fill=(255, 255, 255))

    img.save(f"{GUI_PATH}/ammo_press.png")
    print("Created: ammo_press.png")


def create_atmospheric_stabilizer_gui():
    """Atmospheric Stabilizer: area effect machine"""
    img, draw = create_base_gui()

    # Energy bar
    draw_energy_bar_bg(draw, 8, 18, 14, 52)

    # Central status area (no slots, just display)
    # Draw a "radar" style circle
    cx, cy = 88, 45
    for r in range(5, 35, 8):
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(100, 150, 100))

    # Range indicator slot (for range upgrades)
    draw_slot(draw, 140, 25)

    # Speed upgrade slot
    draw_slot(draw, 140, 45)

    # Overlays
    for y in range(52):
        r = int(50 + y * 1.5)
        g = int(150 - y * 0.5)
        draw.line([176, y, 189, y], fill=(50, max(50, g), 50))

    img.save(f"{GUI_PATH}/atmospheric_stabilizer.png")
    print("Created: atmospheric_stabilizer.png")


def main():
    create_crusher_gui()
    create_crucible_gui()
    create_washer_gui()
    create_cobble_generator_gui()
    create_obsidian_generator_gui()
    create_deconstructor_gui()
    create_auto_repairer_gui()
    create_ammo_press_gui()
    create_atmospheric_stabilizer_gui()
    print("\nAll GUI textures created successfully!")


if __name__ == "__main__":
    main()
