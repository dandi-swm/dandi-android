"""냐미 정면 앉은 자세 공식 일러스트(체형 5종)를 리그용 파츠와 rig.json으로 분해한다.

윤곽선(어두운 선)을 경계로 연결 요소를 나눠 머리, 몸, 반다나, 꼬리를 얻고,
귀는 수동으로 준 절단선으로 머리에서 떼어 낸다. 눈, 입은 머리 안의 어두운 덩어리로 찾는다.
뒤 레이어는 앞 레이어 밑으로 색을 연장해 움직여도 빈틈이 보이지 않게 한다(이음새 방지).

좌표는 리그 논리 좌표(136 x 136 u, 바닥 앵커 (64, 118))로 바꿔 rig.json에 쓴다.
배율은 NORMAL의 귀 끝~발바닥 높이를 스프라이트 실측 98u에 맞춘 값 하나를 체형 5종에 똑같이 쓴다.

    python3 -I tools/cat-rig/extract_parts.py <공식 일러스트 폴더> [출력 폴더]

공식 일러스트 폴더 예: ~/Dandi-SWM/dandi-character-assets/character (nyami_brand_hero_{-2..2}.png)
출력 폴더 기본값: cat/data/src/main/assets/cats/rig/parts/nyami
필요한 패키지: numpy, opencv-python-headless, scipy, pillow
"""
import json
import os
import sys

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC = ''  # main에서 정한다
BODIES = {'LEAN': '-2', 'SLIM': '-1', 'NORMAL': '0', 'CHUBBY': '1', 'PLUMP': '2'}

# 귀 절단선(원본 좌표). 바깥 실루엣 쪽 A에서 머리 꼭대기 쪽 B 순서.
EAR_CUTS = {
    'NORMAL': {
        'earL': [(262, 305), (300, 268), (345, 232), (410, 203), (497, 150)],
        'earR': [(932, 305), (895, 268), (855, 232), (785, 203), (700, 150)],
    },
    'LEAN': {
        'earL': [(262, 383), (296, 342), (326, 314), (356, 294), (401, 253), (478, 215)],
        'earR': [(900, 384), (869, 342), (839, 314), (809, 294), (766, 253), (689, 215)],
    },
    'SLIM': {
        'earL': [(284, 311), (316, 270), (353, 228), (403, 200), (443, 175), (497, 151)],
        'earR': [(933, 311), (900, 270), (863, 228), (813, 200), (773, 175), (718, 151)],
    },
    'CHUBBY': {
        'earL': [(301, 269), (334, 234), (376, 198), (421, 175), (456, 159), (529, 125)],
        'earR': [(959, 269), (926, 234), (884, 198), (839, 175), (804, 159), (731, 125)],
    },
    'PLUMP': {
        'earL': [(328, 226), (362, 201), (410, 173), (452, 153), (485, 139), (550, 113)],
        'earR': [(967, 226), (937, 201), (890, 173), (847, 153), (815, 139), (750, 113)],
    },
}

SCALE = 0.5          # 출력 텍스처 배율
PAD = 6              # 레이어 여백(출력 픽셀)


def load(body):
    path = SRC + 'nyami_brand_hero_%s.png' % BODIES[body]
    return np.array(Image.open(path).convert('RGBA'))


def poly_mask(shape, pts):
    m = np.zeros(shape, np.uint8)
    cv2.fillPoly(m, [np.array(pts, np.int32)], 1)
    return m.astype(bool)


def ear_polygon(cut, side, shape):
    """절단선 위쪽(귀 쪽)을 덮는 다각형."""
    h, w = shape
    a, b = cut[0], cut[-1]
    if side == 'L':
        return list(cut) + [(b[0] + 12, -60), (-60, -60), (-60, a[1])]
    return list(cut) + [(b[0] - 12, -60), (w + 60, -60), (w + 60, a[1])]


def nearest_fill(rgb, src_mask, dst_mask, blur=9):
    """dst_mask 영역을 src_mask의 가장 가까운 픽셀 색으로 채운다."""
    idx = ndi.distance_transform_edt(~src_mask, return_distances=False, return_indices=True)
    out = rgb.copy()
    ys, xs = np.where(dst_mask)
    out[ys, xs] = rgb[idx[0][ys, xs], idx[1][ys, xs]]
    if blur:
        sm = cv2.GaussianBlur(out, (0, 0), blur)
        out[dst_mask] = sm[dst_mask]
    return out


def extract(body, out_dir, debug_dir=None):
    os.makedirs(out_dir, exist_ok=True)
    rgba = load(body)
    H, W = rgba.shape[:2]
    rgb = rgba[..., :3].copy()
    alpha = rgba[..., 3].astype(np.float32) / 255.0
    alpha = np.clip(alpha / (253.0 / 255.0), 0, 1)   # 원본 불투명도가 253이라 1로 맞춘다
    alpha[alpha < 0.06] = 0                          # 바탕에 깔린 미세한 알파 잡음 제거
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    Hh, Ss, Vv = hsv[..., 0].astype(int), hsv[..., 1].astype(int), hsv[..., 2].astype(int)
    solid = alpha > 0.5
    dark = solid & (Vv < 170)
    free = solid & ~dark

    n, lab, st, ce = cv2.connectedComponentsWithStats(free.astype(np.uint8), connectivity=4)
    comps = [dict(k=k, area=int(st[k, 4]), bbox=st[k, :4].tolist(), c=ce[k], mean=rgb[lab == k].mean(0))
             for k in range(1, n) if st[k, 4] >= 200]
    comps.sort(key=lambda c: -c['area'])

    def greenish(m):
        return m[1] >= m[0] - 2 and m[1] > m[2] + 10

    green = [c for c in comps if greenish(c['mean'])]
    fur = [c for c in comps if not greenish(c['mean'])]
    big = fur[:2]
    head_c = min(big, key=lambda c: c['c'][1])
    body_c = max(big, key=lambda c: c['c'][1])
    tail_c = [c for c in fur[2:] if c['area'] > 8000][0]
    region = np.zeros((H, W), np.uint8)      # 1 head 2 body 3 bandana 4 tail
    region[lab == head_c['k']] = 1
    region[lab == body_c['k']] = 2
    region[lab == tail_c['k']] = 4
    for c in green:
        region[lab == c['k']] = 3

    # 남은 작은 밝은 조각은 가장 가까운 큰 부위로
    rest = free & (region == 0)
    if rest.any():
        idx = ndi.distance_transform_edt(region == 0, return_distances=False, return_indices=True)
        region[rest] = region[idx[0][rest], idx[1][rest]]

    # ---- 눈·입: 머리 안에 갇힌 어두운 덩어리
    nd, dlab, dst, dce = cv2.connectedComponentsWithStats(dark.astype(np.uint8), connectivity=8)
    head_fill = ndi.binary_fill_holes(region == 1)
    eyes = []
    inner_dark = []
    for k in range(1, nd):
        x, y, w, h, ar = dst[k]
        m = dlab == k
        if ar < 40:
            continue
        inside = head_fill[m].mean()
        if inside > 0.98 and ar < 40000:
            inner_dark.append((k, ar, dce[k], (x, y, w, h)))
    inner_dark.sort(key=lambda t: -t[1])
    eye_ks = inner_dark[:2]
    eye_ks.sort(key=lambda t: t[2][0])
    (kL, _, cL, bL), (kR, _, cR, bR) = eye_ks
    eye_dist = cR[0] - cL[0]
    eye_y = (cL[1] + cR[1]) / 2
    face_cx = (cL[0] + cR[0]) / 2
    mouth_mask = np.zeros((H, W), bool)
    for k, ar, c, bb in inner_dark[2:]:
        if abs(c[0] - face_cx) < eye_dist * 0.38 and eye_y - 10 < c[1] < eye_y + eye_dist * 0.75:
            mouth_mask |= dlab == k
    mouth_mask = ndi.binary_fill_holes(mouth_mask)
    eyeL_mask = ndi.binary_fill_holes(dlab == kL)
    eyeR_mask = ndi.binary_fill_holes(dlab == kR)
    face_parts = {'eyeL': eyeL_mask, 'eyeR': eyeR_mask, 'mouth': mouth_mask}

    # ---- 윤곽선 픽셀을 부위에 배정
    line = dark & ~eyeL_mask & ~eyeR_mask & ~mouth_mask
    dist = {r: cv2.distanceTransform((region != r).astype(np.uint8), cv2.DIST_L2, 5) for r in (1, 2, 3, 4)}
    green_line = line & (Hh >= 22) & (Hh <= 90) & (Ss > 20)
    R = 13.0
    assign = np.zeros((H, W), np.uint8)
    dmin = np.minimum.reduce([dist[1], dist[2], dist[3], dist[4]])
    # 초록 선은 반다나, 갈색 선은 털 부위 가운데 앞에 있는 쪽(머리 > 몸 > 꼬리)
    for r in (4, 2, 1):
        ok = line & ~green_line & (dist[r] <= np.maximum(R, dmin + 0.5))
        assign[ok] = r
    near_fur = np.minimum.reduce([dist[1], dist[2], dist[4]])
    g_ok = line & green_line & (dist[3] <= R + 6)
    assign[g_ok] = 3
    left = line & (assign == 0)
    if left.any():
        stack = np.stack([dist[1], dist[2], dist[3], dist[4]], 0)
        assign[left] = (np.argmin(stack[:, left], 0) + 1).astype(np.uint8)
    region[line] = assign[line]
    for m in face_parts.values():
        region[m] = 1

    # 반투명 가장자리(알파 0~0.5)는 가장 가까운 부위로
    edge = (alpha > 0) & (region == 0)
    idx = ndi.distance_transform_edt(region == 0, return_distances=False, return_indices=True)
    region[edge] = region[idx[0][edge], idx[1][edge]]

    # ---- 귀
    cuts = EAR_CUTS[body]
    ear_poly = {s: poly_mask((H, W), ear_polygon(cuts['ear' + s], s, (H, W))) for s in 'LR'}
    head_all = region == 1
    ear_core = {s: head_all & ear_poly[s] for s in 'LR'}
    ear_ext = {}
    for s in 'LR':
        k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (61, 61))
        dil = cv2.dilate(ear_poly[s].astype(np.uint8), k).astype(bool)
        ear_ext[s] = head_all & dil
    any_ear = ear_poly['L'] | ear_poly['R']
    d_out = cv2.distanceTransform((~any_ear).astype(np.uint8), cv2.DIST_L2, 5)
    head_feather = np.clip(d_out / 20.0, 0, 1)
    head_feather = head_feather * head_feather * (3 - 2 * head_feather)

    # ---- 머리 밑그림: 눈·입 자리를 털색으로 메운다
    hole = np.zeros((H, W), np.uint8)
    for m in face_parts.values():
        hole |= cv2.dilate(m.astype(np.uint8), np.ones((9, 9), np.uint8))
    head_rgb = cv2.inpaint(rgb, hole, 7, cv2.INPAINT_TELEA)

    def feather(mask, px=1.2):
        return np.clip(cv2.GaussianBlur(mask.astype(np.float32), (0, 0), px) * 1.0, 0, 1)

    def inner(mask, px=4):
        """가려지는 연장 영역의 알파. 실루엣 밖으로 번지지 않게 안쪽으로 줄인 뒤 부드럽게 한다."""
        k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * px + 1, 2 * px + 1))
        er = cv2.erode(mask.astype(np.uint8), k)
        return np.clip(cv2.GaussianBlur(er.astype(np.float32), (0, 0), 1.2), 0, 1) * (alpha > 0.98)

    near_dark = cv2.dilate(dark.astype(np.uint8), np.ones((15, 15), np.uint8)).astype(bool)
    layers = {}

    def add(name, color, a, z):
        layers[name] = dict(rgb=color, a=np.clip(a, 0, 1).astype(np.float32), z=z)

    # 앞 레이어들
    add('head', head_rgb, alpha * feather(head_all) * head_feather, 60)
    for s in 'LR':
        add('ear' + s, rgb, alpha * feather(ear_ext[s]), 50)
    for name, m in face_parts.items():
        grow = cv2.dilate(m.astype(np.uint8), np.ones((5, 5), np.uint8)).astype(bool)
        add(name, rgb, feather(grow, 1.0) * (feather(m, 1.6) > 0.02) * np.clip(feather(m, 1.2) * 1.6, 0, 1), 70)

    # 반다나: 머리 밑으로 위쪽 연장
    band = region == 3
    band_src = band & ~near_dark
    k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (91, 91))
    band_dst = cv2.dilate(band.astype(np.uint8), k).astype(bool) & head_all
    band_rgb = nearest_fill(rgb, band_src, band_dst)
    add('bandana', band_rgb, np.maximum(alpha * feather(band), inner(band_dst)), 40)

    # 몸: 반다나·머리 밑으로 연장
    bod = region == 2
    bod_src = bod & ~near_dark
    k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (241, 241))
    bod_dst = cv2.dilate(bod.astype(np.uint8), k).astype(bool) & (band | head_all)
    # 머리 쪽으로는 너무 올라가지 않게(목 높이까지만)
    ys = np.where(band.any(1))[0]
    neck_top = ys.min() - 20
    bod_dst[:max(neck_top, 0)] = False
    main_band_mask = lab == max(green, key=lambda c: c['area'])['k']
    hull_pts = cv2.findNonZero((bod | main_band_mask).astype(np.uint8))
    hull = cv2.convexHull(hull_pts)
    hull_m = np.zeros((H, W), np.uint8)
    cv2.fillConvexPoly(hull_m, hull, 1)
    bod_dst &= hull_m.astype(bool)
    bod_rgb = nearest_fill(rgb, bod_src, bod_dst, blur=12)
    add('body', bod_rgb, np.maximum(alpha * feather(bod), inner(bod_dst)), 30)

    # 꼬리: 몸 밑으로 연장
    tl = region == 4
    tl_src = tl & ~near_dark
    k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (81, 81))
    tl_dst = cv2.dilate(tl.astype(np.uint8), k).astype(bool) & (bod | bod_dst)
    tl_rgb = nearest_fill(rgb, tl_src, tl_dst, blur=10)
    add('tail', tl_rgb, np.maximum(alpha * feather(tl), inner(tl_dst)), 20)

    # ---- 기준점
    def bbox_of(m):
        ys, xs = np.where(m)
        return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1

    hb = bbox_of(head_all & ~any_ear)
    bb = bbox_of(bod)
    tb = bbox_of(tl)
    bnb = bbox_of(band)
    ground = bb[3]
    # 꼬리 뿌리: 꼬리에서 몸 중심에 가장 가까운 점, 끝: 뿌리에서 가장 먼 점
    ys, xs = np.where(tl)
    bcx, bcy = (bb[0] + bb[2]) / 2, bb[3] - (bb[3] - bb[1]) * 0.18
    d = (xs - bcx) ** 2 + (ys - bcy) ** 2
    i0 = int(np.argmin(d))
    tbase = (float(xs[i0]), float(ys[i0]))
    d2 = (xs - tbase[0]) ** 2 + (ys - tbase[1]) ** 2
    i1 = int(np.argmax(d2))
    ttip = (float(xs[i1]), float(ys[i1]))

    def eye_info(m):
        x0, y0, x1, y1 = bbox_of(m)
        return dict(cx=(x0 + x1) / 2, cy=(y0 + y1) / 2, rx=(x1 - x0) / 2, ry=(y1 - y0) / 2)

    mb = bbox_of(mouth_mask)
    # 원화 색: 윤곽선, 눈동자, 볼터치(코드로 그리는 감은 눈 선, 입, 그림자에 쓴다)
    def median_rgb(mask):
        px = rgb[mask & solid]
        return [int(v) for v in np.median(px, axis=0)] if len(px) else [0, 0, 0]
    pupil_mask = (eyeL_mask | eyeR_mask) & (Vv < 80)
    head_core = region == 1
    blush_mask = head_core & (rgb[..., 0].astype(int) > 200) & (rgb[..., 0].astype(int) - rgb[..., 1].astype(int) > 45) & (Ss > 60)
    palette = dict(line=median_rgb(line), pupil=median_rgb(pupil_mask), blush=median_rgb(blush_mask))
    # 반다나 매듭: 반다나 초록 조각 가운데 주 조각을 뺀 것들의 중심
    main_band = max(green, key=lambda c: c['area'])
    knot_cx = float(np.mean([c['c'][0] for c in green if c is not main_band])) if len(green) > 1 else bnb[2]
    knot_cy = float(np.mean([c['c'][1] for c in green if c is not main_band])) if len(green) > 1 else bnb[1] + 60
    meta = dict(
        body=body, size=[W, H], scale=SCALE, ground=ground,
        head=dict(bbox=hb, cx=(hb[0] + hb[2]) / 2, cy=(hb[1] + hb[3]) / 2),
        bodyBox=bb, tailBox=tb, bandanaBox=bnb,
        neck=[face_cx, float(bnb[1]) + 25],
        eyeL=eye_info(eyeL_mask), eyeR=eye_info(eyeR_mask),
        mouth=dict(cx=(mb[0] + mb[2]) / 2, cy=(mb[1] + mb[3]) / 2, w=mb[2] - mb[0], h=mb[3] - mb[1]),
        tailBase=tbase, tailTip=ttip, knotX=knot_cx, knotY=knot_cy,
        earL=dict(cut=cuts['earL']), earR=dict(cut=cuts['earR']),
        palette=palette,
        layers={},
    )
    for s in 'LR':
        ys, xs = np.where(ear_core[s])
        top = int(np.argmin(ys))
        meta['ear' + s]['tip'] = [float(xs[top]), float(ys[top])]

    # ---- 저장 (크롭 + 축소)
    debug = np.zeros((H, W, 4), np.float32)
    for name, L in sorted(layers.items(), key=lambda kv: kv[1]['z']):
        a = L['a']
        ys, xs = np.where(a > 0.004)
        x0, y0, x1, y1 = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
        pad = int(PAD / SCALE)
        x0, y0, x1, y1 = max(x0 - pad, 0), max(y0 - pad, 0), min(x1 + pad, W), min(y1 + pad, H)
        crop_rgb = L['rgb'][y0:y1, x0:x1].astype(np.float32)
        crop_a = a[y0:y1, x0:x1]
        # 투명한 곳의 색을 주변 색으로 번지게 해 축소·필터링 때 테두리가 어두워지지 않게
        opaque = crop_a > 0.5
        if opaque.any():
            idx = ndi.distance_transform_edt(~opaque, return_distances=False, return_indices=True)
            bleed = crop_rgb[idx[0], idx[1]]
            crop_rgb = np.where(crop_a[..., None] > 0.5, crop_rgb, bleed)
        out = np.dstack([crop_rgb, crop_a * 255]).clip(0, 255).astype(np.uint8)
        ow, oh = max(1, int(round((x1 - x0) * SCALE))), max(1, int(round((y1 - y0) * SCALE)))
        pm = out.astype(np.float32)
        pm[..., :3] *= pm[..., 3:4] / 255.0
        small = cv2.resize(pm, (ow, oh), interpolation=cv2.INTER_AREA)
        sa = small[..., 3:4]
        small[..., :3] = np.where(sa > 0, small[..., :3] / np.maximum(sa, 1e-3) * 255.0, 0)
        small = small.clip(0, 255).astype(np.uint8)
        Image.fromarray(small).save(os.path.join(out_dir, name + '.webp'), 'WEBP', lossless=True, quality=100, method=6)
        meta['layers'][name] = dict(x=int(x0), y=int(y0), w=int(x1 - x0), h=int(y1 - y0), z=L['z'])
        # 합성 검증용
        full = np.zeros((H, W, 4), np.float32)
        full[y0:y1, x0:x1, :3] = crop_rgb
        full[y0:y1, x0:x1, 3] = crop_a
        fa = full[..., 3:4]
        debug[..., :3] = full[..., :3] * fa + debug[..., :3] * (1 - fa)
        debug[..., 3:4] = fa + debug[..., 3:4] * (1 - fa)

    # 재합성과 원본 차이
    orig = rgba[..., :3].astype(np.float32)
    diff = np.abs(debug[..., :3] - orig) * (alpha[..., None] > 0.9)
    print(body, 'recompose max diff', float(diff.max()), 'mean', float(diff.mean()),
          'alpha diff', float(np.abs(debug[..., 3] - alpha).max()))
    if not debug_dir:
        return meta
    os.makedirs(debug_dir, exist_ok=True)
    comp = np.dstack([debug[..., :3], debug[..., 3] * 255]).clip(0, 255).astype(np.uint8)
    Image.fromarray(comp).save(os.path.join(debug_dir, body + '_recomposed.png'))
    vis = np.zeros((H, W, 3), np.uint8) + 235
    cols = {1: (120, 200, 160), 2: (240, 120, 120), 3: (190, 150, 230), 4: (200, 200, 120)}
    for r, c in cols.items():
        vis[region == r] = c
    vis[ear_core['L'] | ear_core['R']] = (110, 160, 240)
    for m in face_parts.values():
        vis[m] = (30, 30, 30)
    vis[line] = (vis[line] * 0.55).astype(np.uint8)
    Image.fromarray(vis).save(os.path.join(debug_dir, body + '_regions.png'))
    return meta


# ---- 리그 논리 좌표(u)로 바꾸기 ----
RIG_ANCHOR = (64.0, 118.0)      # 바닥 앵커(목 x, 발바닥 y)
SPRITE_HEIGHT_U = 98.0          # 스프라이트 실측 NORMAL 귀 끝~발바닥 높이
FILES = {'LEAN': '-2', 'SLIM': '-1', 'NORMAL': '0', 'CHUBBY': '1', 'PLUMP': '2'}


def to_rig(meta, scale):
    """원본 px 기준 meta를 rig.json 형식(u)으로 바꾼다."""
    ax, ay = meta['neck'][0], meta['ground']

    def pt(p):
        return [round((p[0] - ax) * scale + RIG_ANCHOR[0], 3), round((p[1] - ay) * scale + RIG_ANCHOR[1], 3)]

    def box(b):
        x0, y0 = pt((b[0], b[1]))
        x1, y1 = pt((b[2], b[3]))
        return [x0, y0, x1, y1]

    def eye(e):
        c = pt((e['cx'], e['cy']))
        return dict(center=c, radiusX=round(e['rx'] * scale, 3), radiusY=round(e['ry'] * scale, 3))

    layers = []
    for name, l in sorted(meta['layers'].items(), key=lambda kv: (kv[1]['z'], kv[0])):
        x, y = pt((l['x'], l['y']))
        layers.append(dict(id=name, file=name + '.webp', x=x, y=y,
                           w=round(l['w'] * scale, 3), h=round(l['h'] * scale, 3)))
    m = meta['mouth']
    return dict(
        version=1,
        body=meta['body'],
        unitsPerSourcePx=round(scale, 6),
        ground=RIG_ANCHOR[1],
        neck=pt(meta['neck']),
        headBox=box(meta['head']['bbox']),
        bodyBox=box(meta['bodyBox']),
        bandanaBox=box(meta['bandanaBox']),
        tailBase=pt(meta['tailBase']),
        tailTip=pt(meta['tailTip']),
        knot=pt((meta['knotX'], meta['knotY'])),
        earL=dict(base=pt(meta['earL']['cut'][0]), top=pt(meta['earL']['cut'][-1]), tip=pt(meta['earL']['tip'])),
        earR=dict(base=pt(meta['earR']['cut'][0]), top=pt(meta['earR']['cut'][-1]), tip=pt(meta['earR']['tip'])),
        eyeL=eye(meta['eyeL']),
        eyeR=eye(meta['eyeR']),
        mouth=dict(center=pt((m['cx'], m['cy'])), w=round(m['w'] * scale, 3), h=round(m['h'] * scale, 3)),
        palette=meta['palette'],
        layers=layers,
    )


if __name__ == '__main__':
    src = os.path.expanduser(sys.argv[1]).rstrip('/') + '/'
    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.dirname(os.path.dirname(here))
    out_root = sys.argv[2] if len(sys.argv) > 2 else os.path.join(repo, 'cat/data/src/main/assets/cats/rig/parts/nyami')
    debug_dir = os.environ.get('CAT_RIG_DEBUG_DIR')
    SRC = src
    BODIES.update(FILES)
    metas = {}
    for body in ['NORMAL', 'LEAN', 'SLIM', 'CHUBBY', 'PLUMP']:
        metas[body] = extract(body, os.path.join(out_root, body), debug_dir)
    n = metas['NORMAL']
    top = min(n['earL']['tip'][1], n['earR']['tip'][1])
    scale = SPRITE_HEIGHT_U / (n['ground'] - top)
    for body, meta in metas.items():
        rig = to_rig(meta, scale)
        with open(os.path.join(out_root, body, 'rig.json'), 'w') as f:
            json.dump(rig, f, ensure_ascii=False, indent=1)
            f.write('\n')
        print(body, 'scale', round(scale, 5), 'layers', len(rig['layers']))
