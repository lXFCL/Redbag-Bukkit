from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parent
RAW = ROOT / "raw"
SIZE = (1600, 900)


PALETTE = {
    "bg": (13, 19, 31),
    "panel": (25, 35, 54),
    "panel_2": (20, 29, 45),
    "text": (244, 247, 252),
    "muted": (177, 188, 205),
    "line": (65, 77, 98),
    "red": (244, 65, 80),
    "gold": (255, 188, 69),
    "blue": (73, 181, 222),
    "green": (72, 200, 138),
    "purple": (171, 113, 255),
}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


F = {
    "eyebrow": font(24),
    "title": font(66, True),
    "h1": font(54, True),
    "h2": font(34, True),
    "h3": font(28, True),
    "body": font(25),
    "small": font(21),
    "mono": font(31, True),
    "num": font(72, True),
}


def rounded_rect(draw: ImageDraw.ImageDraw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def text(draw: ImageDraw.ImageDraw, xy, value, font_key="body", fill=None, anchor=None):
    draw.text(xy, value, font=F[font_key], fill=fill or PALETTE["text"], anchor=anchor)


def text_size(draw: ImageDraw.ImageDraw, value, font_key="body"):
    box = draw.textbbox((0, 0), value, font=F[font_key])
    return box[2] - box[0], box[3] - box[1]


def pill(draw, x, y, label, color, pad_x=24, pad_y=10):
    w, h = text_size(draw, label, "small")
    rounded_rect(draw, (x, y, x + w + pad_x * 2, y + h + pad_y * 2), 18, color)
    text(draw, (x + pad_x, y + pad_y - 1), label, "small", (255, 255, 255))
    return x + w + pad_x * 2 + 14


def background(accent=(73, 181, 222)):
    img = Image.new("RGB", SIZE, PALETTE["bg"])
    d = ImageDraw.Draw(img, "RGBA")
    for x in range(0, SIZE[0], 80):
        d.line((x, 0, x, SIZE[1]), fill=(255, 255, 255, 14), width=1)
    for y in range(0, SIZE[1], 80):
        d.line((0, y, SIZE[0], y), fill=(255, 255, 255, 14), width=1)
    for offset, alpha in [(-580, 32), (-150, 24), (310, 22)]:
        d.polygon(
            [
                (offset, SIZE[1]),
                (offset + 190, SIZE[1]),
                (offset + 1090, 0),
                (offset + 900, 0),
            ],
            fill=(*accent, alpha),
        )
    random.seed(8)
    colors = [PALETTE["red"], PALETTE["gold"], PALETTE["blue"], PALETTE["green"]]
    for _ in range(36):
        x = random.randint(55, SIZE[0] - 55)
        y = random.randint(50, SIZE[1] - 50)
        c = random.choice(colors)
        d.rounded_rectangle((x, y, x + random.randint(7, 18), y + random.randint(7, 18)), 4, fill=(*c, 44))
    return img


def draw_header(d, title_value, subtitle, tag="功能说明"):
    text(d, (92, 86), tag, "eyebrow", PALETTE["gold"])
    text(d, (90, 122), title_value, "title")
    text(d, (94, 206), subtitle, "body", PALETTE["muted"])


def redbag_icon(size=220):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    s = size
    d.rounded_rectangle((34, 50, s - 34, s - 20), 24, fill=(224, 43, 55), outline=(255, 197, 76), width=5)
    d.polygon([(45, 58), (s // 2, 116), (s - 45, 58)], fill=(255, 70, 82), outline=(255, 197, 76))
    d.ellipse((70, 94, s - 70, s - 42), fill=(255, 188, 69))
    text(d, (s // 2, 111), "福", "h1", (130, 43, 22), anchor="mm")
    return img


def paste_shadow(base, item, xy, shadow=18):
    x, y = xy
    alpha = item.split()[-1]
    sh = Image.new("RGBA", item.size, (0, 0, 0, 170))
    sh.putalpha(alpha.filter(ImageFilter.GaussianBlur(shadow)))
    base.alpha_composite(sh, (x + 12, y + 14))
    base.alpha_composite(item, xy)


def overview():
    img = background(PALETTE["red"]).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    draw_header(d, "红包互动插件", "给服主看的功能总览：两种红包、领取规则、全服反馈与配置项")
    paste_shadow(img, redbag_icon(220), (1110, 95))

    x = 92
    y = 286
    for label, color in [("手速红包", PALETTE["red"]), ("口令红包", PALETTE["gold"]), ("GUI 选物品", PALETTE["blue"]), ("配置驱动", PALETTE["green"])]:
        x = pill(d, x, y, label, color)

    steps = [
        ("1", "创建红包", "填写金额、份数、祝福语", PALETTE["red"]),
        ("2", "确认物品", "GUI 选择领取物品", PALETTE["gold"]),
        ("3", "玩家领取", "左键物品或输入口令", PALETTE["blue"]),
        ("4", "公开反馈", "广播数量和领取结果", PALETTE["green"]),
    ]
    left = 78
    top = 505
    gap = 30
    w = 345
    h = 180
    for i, (num, title_value, body, color) in enumerate(steps):
        bx = left + i * (w + gap)
        rounded_rect(d, (bx, top, bx + w, top + h), 18, (*PALETTE["panel"], 245), outline=color, width=2)
        d.ellipse((bx + 34, top + 42, bx + 108, top + 116), fill=color)
        text(d, (bx + 71, top + 80), num, "num", (10, 18, 30), anchor="mm")
        text(d, (bx + 132, top + 58), title_value, "h3")
        text(d, (bx + 132, top + 100), body, "small", PALETTE["muted"])
        if i < len(steps) - 1:
            d.line((bx + w, top + 88, bx + w + gap, top + 88), fill=(*PALETTE["line"], 220), width=3)

    rounded_rect(d, (94, 724, 725, 826), 18, (*PALETTE["panel_2"], 245), outline=(*PALETTE["blue"], 210), width=1)
    text(d, (125, 744), "/redbag send 10000 10 祝大家发财", "mono", PALETTE["gold"])
    text(d, (125, 787), "发送手速红包，随后在 GUI 中选择领取物品", "small", PALETTE["muted"])
    rounded_rect(d, (770, 724, 1410, 826), 18, (*PALETTE["panel_2"], 245), outline=(*PALETTE["green"], 210), width=1)
    text(d, (801, 744), "/redbag code 100 1 恭喜发财", "mono", PALETTE["green"])
    text(d, (801, 787), "发送口令红包，玩家输入口令即可领取", "small", PALETTE["muted"])
    img.convert("RGB").save(ROOT / "01-overview.png", quality=95)


def gui_crop():
    src = Image.open(RAW / "redbag-gui.png").convert("RGB")
    return src.crop((585, 185, 1325, 884))


def item_gui():
    img = background(PALETTE["gold"]).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    draw_header(d, "发送手速红包", "输入指令后打开完整物品选择 GUI，选中物品后发布")

    rounded_rect(d, (92, 300, 622, 398), 16, (*PALETTE["panel"], 245), outline=(*PALETTE["gold"], 230), width=2)
    text(d, (125, 326), "/redbag send 10000 10 祝大家发财", "mono", PALETTE["gold"])
    text(d, (125, 366), "金额、份数和祝福语在指令中填写", "small", PALETTE["muted"])

    items = [
        ("泥土", PALETTE["green"]),
        ("钻石块", PALETTE["blue"]),
        ("水桶", (86, 130, 230)),
        ("红蘑菇", PALETTE["red"]),
        ("金锭", PALETTE["gold"]),
        ("绿宝石", PALETTE["green"]),
    ]
    x0, y0 = 100, 455
    for idx, (label, color) in enumerate(items):
        row = idx // 3
        col = idx % 3
        bx = x0 + col * 182
        by = y0 + row * 92
        rounded_rect(d, (bx, by, bx + 155, by + 66), 18, color)
        text(d, (bx + 77, by + 33), label, "body", (255, 255, 255), anchor="mm")

    for i, (title_value, body, color) in enumerate(
        [
            ("默认 10 种物品", "可在配置中替换", PALETTE["blue"]),
            ("点击即确认", "不会要求输入材质名", PALETTE["green"]),
        ]
    ):
        bx = 94 + i * 286
        by = 685
        rounded_rect(d, (bx, by, bx + 250, by + 112), 16, (*PALETTE["panel"], 245), outline=color, width=1)
        d.rounded_rectangle((bx + 24, by + 26, bx + 62, by + 64), 10, fill=color)
        text(d, (bx + 78, by + 28), title_value, "h3")
        text(d, (bx + 24, by + 76), body, "small", PALETTE["muted"])

    crop = gui_crop()
    crop.thumbnail((735, 680), Image.Resampling.LANCZOS)
    frame = Image.new("RGBA", (crop.width + 36, crop.height + 36), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame, "RGBA")
    rounded_rect(fd, (0, 0, frame.width - 1, frame.height - 1), 18, (11, 16, 24, 235), outline=(*PALETTE["gold"], 220), width=2)
    frame.alpha_composite(crop.convert("RGBA"), (18, 18))
    paste_shadow(img, frame, (745, 165), 20)
    text(d, (762, 828), "完整 GUI 截图：标题、可选物品、背包与快捷栏都保留", "small", PALETTE["muted"])
    img.convert("RGB").save(ROOT / "02-item-gui.png", quality=95)


def left_click():
    img = background(PALETTE["blue"]).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    draw_header(d, "手持指定物品领取", "玩家左键当前手持物品即可参与手速红包，规则由服务端配置决定")

    rounded_rect(d, (98, 306, 850, 665), 22, (*PALETTE["panel"], 246), outline=(*PALETTE["blue"], 230), width=2)
    text(d, (150, 390), "左键", "title", PALETTE["gold"])
    text(d, (150, 500), "抢手速红包", "h1")
    text(d, (153, 575), "插件判断当前正在领取的红包，并按领取顺序分配随机金额", "body", PALETTE["muted"])

    rounded_rect(d, (985, 272, 1368, 654), 22, (*PALETTE["panel"], 246), outline=(*PALETTE["blue"], 210), width=2)
    rounded_rect(d, (1060, 345, 1290, 548), 24, (30, 73, 88), outline=(*PALETTE["blue"], 200), width=1)
    d.polygon([(1112, 397), (1250, 397), (1290, 438), (1188, 438)], fill=(95, 187, 220))
    d.polygon([(1112, 397), (1188, 438), (1188, 502), (1112, 438)], fill=(33, 126, 154))
    d.polygon([(1188, 438), (1290, 438), (1290, 502), (1188, 502)], fill=(67, 160, 188))
    d.rectangle((1161, 456, 1186, 481), fill=(235, 247, 255))
    text(d, (1175, 596), "手持：钻石块", "h3", anchor="mm")

    rounded_rect(d, (126, 716, 845, 790), 16, (*PALETTE["panel_2"], 245), outline=(*PALETTE["gold"], 230), width=1)
    text(d, (160, 739), "速度加成", "h3")
    x = 330
    for color in [PALETTE["green"], PALETTE["blue"], PALETTE["gold"], PALETTE["red"]]:
        rounded_rect(d, (x, 736, x + 62, 774), 11, color)
        x += 86
    text(d, (660, 742), "更快 ≈ 更高上限", "body", PALETTE["gold"])

    rounded_rect(d, (905, 716, 1405, 790), 16, (*PALETTE["panel_2"], 245), outline=(*PALETTE["blue"], 180), width=1)
    text(d, (940, 739), "可配置项", "h3")
    text(d, (1088, 742), "领取物品、有效期、金额上限、提示文案", "small", PALETTE["muted"])
    img.convert("RGB").save(ROOT / "03-left-click-claim.png", quality=95)


def configurable():
    img = background(PALETTE["green"]).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    draw_header(d, "灵活自定义", "规则、提示、领取物品和广播内容都可以按服务器活动调整")

    cards = [
        ("有效期", "手速红包默认 10 分钟\n口令红包默认 5 分钟", PALETTE["gold"]),
        ("物品列表", "泥土、钻石块、水桶等\n都能自由替换", PALETTE["blue"]),
        ("口令红包", "公开发送口令即可领取\n重复口令会被拦截", PALETTE["green"]),
        ("全服广播", "祝福语、剩余数量\n领取结果自动提示", PALETTE["red"]),
    ]
    for i, (title_value, body, color) in enumerate(cards):
        col = i % 2
        row = i // 2
        bx = 92 + col * 400
        by = 292 + row * 194
        rounded_rect(d, (bx, by, bx + 360, by + 144), 16, (*PALETTE["panel"], 246), outline=color, width=1)
        d.rounded_rectangle((bx + 24, by + 24, bx + 62, by + 62), 10, fill=color)
        text(d, (bx + 82, by + 24), title_value, "h2")
        for line_i, line in enumerate(body.splitlines()):
            text(d, (bx + 24, by + 88 + line_i * 32), line, "small", PALETTE["muted"])

    rounded_rect(d, (1010, 170, 1420, 712), 24, (*PALETTE["panel"], 248), outline=(*PALETTE["green"], 235), width=2)
    text(d, (1215, 235), "配置面板", "h2", anchor="mm")
    settings = [
        ("手速红包有效期", "10 分钟", PALETTE["gold"]),
        ("口令红包有效期", "5 分钟", PALETTE["blue"]),
        ("重复口令保护", "开启", PALETTE["green"]),
        ("祝福语广播", "开启", PALETTE["red"]),
        ("领取物品列表", "可编辑", PALETTE["purple"]),
    ]
    y = 318
    for label, value, color in settings:
        rounded_rect(d, (1058, y, 1372, y + 62), 16, (*PALETTE["panel_2"], 250), outline=color, width=1)
        text(d, (1088, y + 18), label, "small", PALETTE["muted"])
        rounded_rect(d, (1238, y + 13, 1358, y + 49), 18, color)
        text(d, (1298, y + 31), value, "small", (255, 255, 255), anchor="mm")
        y += 82

    rounded_rect(d, (106, 716, 860, 790), 16, (*PALETTE["panel"], 246), outline=(95, 112, 135), width=1)
    text(d, (142, 738), "不用改代码，也能把红包玩法调成适合当前活动的规则", "body")
    img.convert("RGB").save(ROOT / "04-configurable.png", quality=95)


def main():
    overview()
    item_gui()
    left_click()
    configurable()


if __name__ == "__main__":
    main()
