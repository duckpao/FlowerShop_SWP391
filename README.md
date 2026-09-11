```bash
git checkout master
git pull origin master
```

**Bước 2: Tạo nhánh làm việc riêng**
Tuyệt đối không code trực tiếp trên nhánh `master`. Tên nhánh cần có tiền tố rõ ràng.
*   Thêm tính năng mới: `feature/ten-tinh-nang`
*   Sửa lỗi: `bugfix/ten-loi`
```bash
git checkout -b feature/ten-tinh-nang
```

**Bước 3: Code và Commit**
Thực hiện công việc và commit thường xuyên. Ghi chú commit cần rõ nghĩa:
```bash
git add .
git commit -m "feat: hoàn thiện API giỏ hàng và tích hợp HMAC-SHA256"
```

**Bước 4: Đẩy code và Tạo Pull Request**
Đẩy nhánh cá nhân lên kho chứa:
```bash
git push origin feature/ten-tinh-nang
```
Sau đó lên giao diện GitHub, tạo một Pull Request (PR) trỏ vào nhánh `master` và tag Bảo để họ kiểm tra.

### 3. Quy tắc tránh Conflict
*   **Giao tiếp:** Phân chia task độc lập, hạn chế 2 người cùng sửa chung một file trong cùng một khoảng thời gian.
*   **Đồng bộ thường xuyên:** Nếu một task kéo dài nhiều ngày, mỗi sáng hãy kéo code từ master về nhánh hiện tại (`git pull origin master`) để đồng bộ và giải quyết conflict sớm.
*   **Kỷ luật Gitignore:** Không commit các file rác sinh ra từ IDE (như `.idea/`) hoặc các thư viện quá nặng (như `node_modules/`).
