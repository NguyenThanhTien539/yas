# 🔄 CI Workflow Synchronization

Script tự động đồng bộ CI workflow files từ template `order-ci.yaml` sang các services khác.

## 🎯 Mục đích

- ✅ Copy format từ `order-ci.yaml` (template chuẩn) sang các service
- ✅ Tự động thay thế tên service (order → location, cart, etc.)
- ✅ Push workflow file lên đúng branch của mỗi service
- ✅ Đồng bộ hàng loạt cho tất cả services

**Lưu ý:** Script CHỈ đồng bộ workflow `.yaml`, KHÔNG sync test cases (test cases pull từ remote vì mỗi service có logic riêng).

---

## 🚀 Cách sử dụng

### Cú pháp cơ bản

```bash
# Đồng bộ 1 service (không push)
python sync-ci-workflow.py <service-name>

# Đồng bộ 1 service và push lên branch
python sync-ci-workflow.py <service-name> --push

# Đồng bộ TẤT CẢ services
python sync-ci-workflow.py --all

# Đồng bộ TẤT CẢ services và push
python sync-ci-workflow.py --all --push
```

### Ví dụ: Đồng bộ location service

```bash
# Bước 1: Generate location-ci.yaml từ template
python sync-ci-workflow.py location

# Bước 2: Review thay đổi
git diff ../.github/workflows/location-ci.yaml

# Bước 3: Push lên branch feature/location_ci_test_case
python sync-ci-workflow.py location --push
```

### Ví dụ: Đồng bộ tất cả services

```bash
# Generate tất cả (review trước)
python sync-ci-workflow.py --all

# Nếu OK, push tất cả lên branches
python sync-ci-workflow.py --all --push
```

---

## 📋 Services được hỗ trợ

| Service | Target Branch | Workflow File |
|---------|--------------|---------------|
| cart | feature/cart_ci_test_case | cart-ci.yaml |
| customer | feature/customer_ci_test_case | customer-ci.yaml |
| inventory | feature/inventory_ci_test_case | inventory-ci.yaml |
| location | feature/location_ci_test_case | location-ci.yaml |
| media | feature/media_ci_test_case | media-ci.yaml |
| product | feature/product_ci_test_case | product-ci.yaml |
| rating | feature/rating_ci_test_case | rating-ci.yaml |
| search | feature/search_ci_test_case | search-ci.yaml |
| tax | feature/tax_ci_test_case | tax-ci.yaml |

---

## 🔧 Script hoạt động như thế nào?

### Automatic Replacements

Script tự động thay thế tên service với **case sensitivity đúng**:

| Template (order) | Generated (location) | Context |
|------------------|---------------------|---------|
| `order service ci` | `location service ci` | Workflow name |
| `"order/**"` | `"location/**"` | Path filter |
| `order-ci.yaml` | `location-ci.yaml` | Workflow file |
| `-pl order` | `-pl location` | Maven module |
| `order/target/site/jacoco` | `location/target/site/jacoco` | Coverage path |
| `Order Coverage Report` | `Location Coverage Report` | Report title (capitalized) |
| `order-jacoco-report` | `location-jacoco-report` | Artifact name |
| `yas-order` | `yas-location` | Snyk project |

**Tổng: ~40+ replacements tự động**

### Workflow khi push (--push flag)

Khi chạy với `--push`, script sẽ:

1. 💾 Stash current changes (nếu có)
2. 🔄 Checkout sang branch target (e.g., `feature/location_ci_test_case`)
3. 📥 Pull latest changes từ remote
4. 📝 Generate workflow file từ template
5. 📦 Stage và commit changes
6. ⬆️ Push lên remote branch
7. 🔙 Quay lại branch gốc
8. 📤 Restore stashed changes

→ **An toàn 100%**: Không mất code uncommitted

---

## 📚 Workflows thực tế

### Scenario 1: First time setup cho service mới

```bash
# Đảm bảo branch tồn tại
git fetch origin
git branch -a | grep location_ci_test_case

# Nếu chưa có, tạo branch
git checkout -b feature/location_ci_test_case
git push -u origin feature/location_ci_test_case
git checkout main

# Đồng bộ workflow
cd synchronize-yaml
python sync-ci-workflow.py location --push

# Verify trên GitHub
# → Check branch feature/location_ci_test_case có commit mới
```

### Scenario 2: Update template và sync lại tất cả

```bash
# 1. Sửa template order-ci.yaml
vim .github/workflows/order-ci.yaml
# Ví dụ: Thêm step security scan mới

# 2. Commit template vào main
git add .github/workflows/order-ci.yaml
git commit -m "feat: Add security scan to CI template"
git push origin main

# 3. Sync template mới sang TẤT CẢ services
cd synchronize-yaml
python sync-ci-workflow.py --all --push

# Kết quả: TẤT CẢ services có cùng security scan step
```

### Scenario 3: Dry-run (review trước khi push)

```bash
# Generate files (không push)
cd synchronize-yaml
python sync-ci-workflow.py --all

# Review tất cả changes
cd ..
git diff .github/workflows/

# Nếu OK
cd synchronize-yaml
python sync-ci-workflow.py --all --push

# Nếu KHÔNG OK, rollback
cd ..
git checkout .github/workflows/
```

---

## ⚙️ Configuration

### Thêm service mới

Edit `sync-ci-workflow.py`, thêm vào list `SERVICES`:

```python
SERVICES = [
    "cart",
    "customer",
    # ... existing services
    "your-new-service",  # ← Add here
]
```

Sau đó chạy:
```bash
python sync-ci-workflow.py your-new-service --push
```

### Customize replacements

Nếu cần thêm custom replacements, edit function `replace_service_name()`:

```python
def replace_service_name(content: str, from_service: str, to_service: str) -> str:
    replacements = [
        # Existing replacements...

        # Your custom replacements
        (f"my-custom-{from_service}", f"my-custom-{to_service}"),
    ]
    # ...
```

---

## ⚠️ Lưu ý quan trọng

### 1. Branch phải tồn tại trước

Script CHỈ push lên branch đã có trên remote. Nếu branch chưa tồn tại:

```bash
git checkout -b feature/new-service_ci_test_case
git push -u origin feature/new-service_ci_test_case
git checkout main
```

### 2. Run từ folder synchronize-yaml

```bash
cd synchronize-yaml
python sync-ci-workflow.py <service>
```

Hoặc từ root:
```bash
cd d:\HCMUS\Third Year\DevOps\Project01\yas\synchronize-yaml
python sync-ci-workflow.py <service>
```

### 3. Template phải là version tốt nhất

File `order-ci.yaml` phải là template chuẩn, đầy đủ nhất. Tất cả services sẽ copy format từ đây.

---

## 🐛 Troubleshooting

### Error: "Remote branch does not exist"

**Nguyên nhân:** Branch chưa được tạo trên remote.

**Giải pháp:**
```bash
git checkout -b feature/service_ci_test_case
git push -u origin feature/service_ci_test_case
git checkout main
```

### Error: "Template file not found"

**Nguyên nhân:** Chạy script không đúng thư mục.

**Giải pháp:**
```bash
cd synchronize-yaml
python sync-ci-workflow.py service
```

### Error: Git conflict

**Nguyên nhân:** Branch có conflict với remote.

**Giải pháp:**
```bash
# Resolve conflict manually
git checkout feature/service_ci_test_case
git pull origin feature/service_ci_test_case
# Fix conflicts
git add .
git commit -m "resolve: Fix merge conflict"
git push

# Chạy lại script
cd synchronize-yaml
python sync-ci-workflow.py service --push
```

### Script crash giữa chừng

**Script tự động rollback:**
- Quay về branch gốc
- Restore stashed changes

**Verify:**
```bash
git status
git stash list
```

---

## 🎓 Best Practices

1. **Luôn update template trước:** Sửa `order-ci.yaml` trước, sau đó sync
2. **Review trước khi push:** Chạy không có `--push` để xem changes
3. **Commit template vào main:** Để track history
4. **Test với 1 service trước:** Rồi mới chạy `--all`
5. **Verify trên GitHub Actions:** Check workflow runs sau khi push
6. **Keep template clean:** Template là single source of truth

---

## 📖 Example Output

### Generate only:
```
📝 Generating workflow for service: location
✅ Generated: ../.github/workflows/location-ci.yaml

✨ Done! Review the changes and use --push to push to branch.
   Command: python sync-ci-workflow.py location --push
```

### Generate + Push:
```
📝 Generating workflow for service: location
✅ Generated: ../.github/workflows/location-ci.yaml

🚀 Pushing to branch: feature/location_ci_test_case
💾 Stashing current changes...
📥 Fetching latest from origin/feature/location_ci_test_case...
🔄 Checking out feature/location_ci_test_case...
📥 Pulling latest changes...
📝 Generating workflow for service: location
✅ Generated: ../.github/workflows/location-ci.yaml
📦 Staging changes...
💾 Committing changes...
⬆️  Pushing to origin/feature/location_ci_test_case...
✅ Successfully pushed ../.github/workflows/location-ci.yaml to feature/location_ci_test_case
🔄 Returning to main...
📤 Restoring stashed changes...

✨ Done! Workflow synced to feature/location_ci_test_case
```

### Batch processing:
```
🔄 Processing all 9 services...

📝 Generating workflow for service: cart
✅ Generated: ../.github/workflows/cart-ci.yaml

... (8 more services)

============================================================
📊 SUMMARY
============================================================
✅ Successfully processed: 9/9 services
============================================================
```

---

## 🚦 Quick Command Reference

```bash
# Single service
python sync-ci-workflow.py location              # Generate only
python sync-ci-workflow.py location --push       # Generate + push

# All services
python sync-ci-workflow.py --all                 # Generate all
python sync-ci-workflow.py --all --push          # Generate + push all

# Review
cd ..
git diff .github/workflows/<service>-ci.yaml

# Rollback
git checkout .github/workflows/<service>-ci.yaml

# Help
python sync-ci-workflow.py
```

---

## 📁 File Structure

```
yas/
├── .github/
│   └── workflows/
│       ├── order-ci.yaml          ← Template (source of truth)
│       ├── location-ci.yaml       ← Generated
│       ├── cart-ci.yaml           ← Generated
│       └── ...
│
└── synchronize-yaml/              ← Script folder
    ├── README.md                  ← This file
    └── sync-ci-workflow.py        ← Main script
```

---

## ✨ Summary

**Script này giúp bạn:**
- ✅ Đồng bộ workflow format từ 1 template chuẩn
- ✅ Tự động thay thế service names với case đúng
- ✅ Push lên đúng branch tự động
- ✅ Batch processing cho nhiều services
- ✅ Safe (auto stash/restore, rollback on error)

**Không làm:**
- ❌ Không sync test cases (test cases pull từ remote)
- ❌ Không overwrite uncommitted work
- ❌ Không push nếu không có --push flag

---

**Bắt đầu ngay:**
```bash
cd synchronize-yaml
python sync-ci-workflow.py location
git diff ../.github/workflows/location-ci.yaml
python sync-ci-workflow.py location --push
```

🎉 **Done!**
