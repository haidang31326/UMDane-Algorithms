# UMDane Online Judge (UMDane OJ)

Nền tảng chấm bài trực tuyến thông minh (Online Judge) kiểu LeetCode, tích hợp Trí tuệ Nhân tạo (Gemini AI) tự động biên soạn đề bài và môi trường chấm điểm bảo mật (Docker Sandbox Engine).

---

## 🚀 Tính năng nổi bật

1. **Chấm bài kiểu LeetCode (Signature-style)**:
   - Người dùng chỉ cần cài đặt thuật toán trong lớp `Solution` có sẵn thay vì viết mã nguồn nhập/xuất tẻ nhạt.
   - Hệ thống tự sinh mã Wrapper (`driverCode` - `Main.java`) để đọc dữ liệu từ Test Case, gọi hàm người dùng và in kết quả.

2. **Sinh đề bài tự động bằng Gemini AI**:
   - Sử dụng Gemini 2.5 Flash API với cấu hình định dạng JSON Schema nghiêm ngặt.
   - Tự động thiết kế đề bài bằng tiếng Việt (tiêu đề, mô tả, gợi ý, ràng buộc dữ liệu constraints), bộ testcases (gồm cả testcases ẩn) và mã nguồn khung mẫu.

3. **Môi trường chạy code bảo mật (Docker Compiler Sandbox)**:
   - Cách ly hoàn toàn mã nguồn người dùng bằng các container siêu nhẹ Alpine JRE.
   - Tắt mạng hoàn toàn (`--network none`) để ngăn mã độc truy cập tài nguyên hoặc gọi API phá hoại.
   - Giới hạn phần cứng nghiêm ngặt (`--memory 128m`, `--cpus 0.5`) chống tấn công tràn ram, treo luồng (Fork Bomb).
   - Đo lường và báo lỗi chi tiết: **Accepted (AC)**, **Wrong Answer (WA)**, **Time Limit Exceeded (TLE)**, **Runtime Error (RE)**, **Compile Error (CE)**.

4. **Tối ưu hiệu năng & Cache**:
   - Tích hợp **Redis Cache** cho các truy vấn lấy thông tin bài toán.
   - Sử dụng cơ chế tuần tự hóa **JSON Serializer** (`RedisSerializer.json()`) thay vì JDK Serializer mặc định, giúp hệ thống hoạt động ổn định và chống crash khi cập nhật DTO lớp dữ liệu.

5. **Trải nghiệm người dùng tiện lợi (UX/UI)**:
   - Giao diện tối hiện đại, hỗ trợ hiệu ứng Glassmorphism và tối ưu tương thích thiết bị.
   - Giao diện viết code Monaco Editor cao cấp (như VS Code).
   - Bộ chọn Test Case mẫu dạng Tab giúp nạp nhanh dữ liệu vào ô console chạy thử.
   - Tự động lưu bản nháp code viết dở vào `localStorage` của trình duyệt.

---

## 🛠️ Công nghệ sử dụng

### 1. Backend
- **Java 21** & **Spring Boot 3.x**
- **Spring Security** & **JWT (JSON Web Token)**
- **Spring Data JPA** & **Flyway Database Migration**
- **Spring Data Redis** & **Spring Cache**
- **Google Generative Language (Gemini REST API)**

### 2. Frontend
- **React.js** (Vite)
- **Monaco Editor** (`@monaco-editor/react`)
- **Lucide Icons**

### 3. DevOps & Sandbox
- **Docker Engine** & **Docker Compose**
- **eclipse-temurin:21-alpine** (Môi trường JDK & JRE)

---

## 📦 Hướng dẫn cài đặt & Chạy ứng dụng

### 1. Cấu hình môi trường
Tạo tệp `.env` tại thư mục gốc cùng cấp với tệp `docker-compose.yml` và điền cấu hình API Key của bạn:
```env
GEMINI_API_KEY=Nhập_API_Key_Gemini_Của_Bạn_Tại_Đây
```

### 2. Chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)
Tại thư mục gốc của dự án, mở Terminal và chạy lệnh:
```bash
docker-compose up --build -d
```
Hệ thống sẽ tự động khởi tạo:
- **MySQL Database**: Cổng `3306`
- **Redis Cache**: Cổng `6379`
- **Backend Service**: Cổng `8080`
- **Frontend Service (Nginx)**: Cổng `3000`

Sau khi container chạy hoàn tất, bạn truy cập giao diện tại địa chỉ: `http://localhost:3000`

---

## 📂 Cấu trúc thư mục dự án

```text
├── src/main/java/com/Dane/UMDane/
│   ├── config/              # Lớp cấu hình (Security, WebSocket, Cache Redis)
│   ├── controller/          # Các endpoint REST API (Auth, Problems, Submissions, Admin)
│   ├── dto/                 # Các đối tượng truyền dữ liệu (Data Transfer Objects)
│   ├── entity/              # Thực thể database JPA (User, Problem, Submission, TestCase)
│   ├── exception/           # Xử lý Exception toàn cục (Global Exception Handler)
│   ├── repository/          # Tương tác cơ sở dữ liệu MySQL
│   ├── security/            # Bộ lọc bảo mật JWT, User details
│   └── service/             # Xử lý logic nghiệp vụ (Sandbox Engine, AI Service, Problem)
├── src/main/resources/
│   ├── db/migration/        # Tệp sql định nghĩa cơ sở dữ liệu nâng cấp qua Flyway
│   └── application.yml      # Tệp cấu hình cấu trúc hệ thống Spring Boot
├── frontend/
│   ├── src/                 # Mã nguồn giao diện React (pages, components, css)
│   ├── package.json         # Danh sách thư viện frontend
│   └── vite.config.js       # Cấu hình máy chủ Dev và Proxy API
├── docker-compose.yml       # Docker cấu hình chạy đa container
└── README.md
```
