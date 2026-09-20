# Ke Hoach Nang Cap Toan Dien RealmFinder (Upgrade Roadmap)

Tai lieu nay luu tru toan bo dinh huong kien truc, tinh nang nang cap va phan tich ky thuat cho RealmFinder.

---

## Phan Tich: Chup va Tai Hien Thuc The / Dong Vat (Entity and Mob Support)

### 1. Co the lam duoc khong?
HOAN TOAN CO THE (100% Kha Thi):
- Minecraft (Fabric 1.21.1) cho phep quet tat ca entity trong AABB goc chup:
  level.getEntities((Entity) null, frustumBox, entity -> frustum.containsPoint(entity.position())).
- Luu tru bang Vanilla NBT CompoundTag: entity.saveWithoutId(new CompoundTag()) + EntityType.getKey(entity.getType()).
- Luu toa do tuong doi theo goc may anh: Vector3f camRel = camera.toCameraSpace(entity.getX(), entity.getY(), entity.getZ()).
- Khi dan anh: snappedCamera.toWorldSpace(...) bien doi sang vi tri moi, xoay goc Yaw cua thuc the theo deltaYaw, gan UUID ngau nhien moi (UUID.randomUUID()) va goi level.addFreshEntity(newEntity).
- Khi Undo: UndoRecord ghi nhan danh sach Entity UUID da spawn de discard() ngay lap tuc.

### 2. Co NEN lam khong?
RAT NEN LAM, nhung can co che an toan (Safety Safeguards):
- Diem hap dan: Tao ra trai nghiem giai do va sinh ton ma thuat tuyet voi (chup lon, bo, cuu, golem, cho meo, thuyen, xe mo, khung tranh, armor stand).
- Quy tac an toan bat buoc:
  1. Blacklist Tuyet Doi: Khong bao gio chup Player, Wither, Ender Dragon (tranh crash mang, desync hoac pha nat the gioi).
  2. Reset UUID: Bat buoc cap UUID moi cho thuc the khi sinh ra de tranh xung dot du lieu the gioi.
  3. Gioi han so luong: Toi da 16 thuc the trong 1 buc anh de tranh giam hieu nang khi nguoi choi chup giua bay thu dong duc.

---

## Lo Trinh Tinh Nang Chi Tiet (Feature Roadmap)

### Giai doan 1: Hoan thien Voxel va Chong Loi (DA HOAN THANH)
- [x] Chup anh Framebuffer sach 100% khong dinh HUD/UI.
- [x] Can goc 90 do (Cardinal Snapping) triet tieu hoan toan goc nghieng meo mo.
- [x] Khoa goc may anh va anh chup qua packet dong bo mang.
- [x] Bao ve tang nen dat (y > minY), loai bo hoan toan cac lo thung tren co va cong trinh.
- [x] He thong Undo hoan tac bang phim Z.
- [x] Giao dien OSD Badge/Pill chong de chu, thanh thuoc goc ngoc luc bao.

### Giai doan 2: Ho tro Thuc the va Dong vat (DA HOAN THANH)
- [x] Quet Entity trong hinh non Frustum (LivingEntity, ItemFrame, ArmorStand, Boat, Minecart).
- [x] Luu NBT thuc the (mau long cuu, yen ngua, mau, ten dat bang NameTag).
- [x] Tai tao thuc the khi dan anh voi toa do, goc quay va cap moi UUID.randomUUID() an toan.
- [x] Tich hop tu dong xoa entity khi bam Undo Z.

### Giai doan 3: Phong to / Thu nho Theo Phoi Canh (DA HOAN THANH)
- [x] Cho phep dung con lan chuot (Ctrl + Scroll hoac Shift + Scroll) va phim tat [ / ] de chon ti le (0.5x Mini, 1.0x Normal, 2.0x Giant, 3.0x Colossal).
- [x] Thuat toan Voxel Scaling: gian no 2 x 2 x 2 cho Giant ma khong tao lo hong hay chong cheo.
- [x] Hien thi nac Scale va so luong thuc the chup duoc tren HUD OSD thoi gian thuc.
- [x] Ti le hoa vi tri xuat hien cua thuc the theo tham so Scale.

### Giai doan 4: Sach Album Anh (Photo Album Item and GUI) (DA HOAN THANH)
- [x] Item Sach Album Anh (realmfinder:photo_album).
- [x] Giao dien GUI hai trang: Trang trai chua 18 o luu anh va tui do nguoi choi; trang phai hien thi anh phong to thoi gian thuc cung thong tin ngay chup, so luong block va so luong thuc the.
- [x] Tu dong loc chi cho phep dat buc anh vao album va luu du lieu an toan vao NBT.

### Giai doan 5: May Anh Chan De Redstone (Tripod Camera Block) (DA BO QUA)
- Giai doan nay da duoc bo qua theo yeu cau cua nguoi dung vi khong can thiet.

### Giai doan 6: Gia Treo Trung Bay Anh (Photo Display Stand / Easel) (DA HOAN THANH)
- [x] Khoi gia ve 3D bang go thong (realmfinder:photo_stand) dat trong the gioi thuc.
- [x] Render 3D Block Entity hien thi truc tiep buc anh mounted len gia ve nghieng 12 do voi tam go do phia sau.
- [x] Tuong tac da dang: Chuot phai de gan hoac lay anh; Shift + Chuot phai de hien thuc hoa buc anh truc tiep ra the gioi theo huong quay cua gia ve.
- [x] Roi ca vat pham gia ve va buc anh an toan khi khoi bi pha huy.

### Giai doan 7: Hologram Ghost Preview (DA BO QUA)
- Giai doan nay da duoc bo qua theo yeu cau cua nguoi dung vi khong can thiet.
