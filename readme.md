# Nền tảng Đấu giá Thời gian thực (Backend)
![](assets\banner.png)
## 1. Tổng quan Dự án

Kho lưu trữ này chứa phần backend cho một nền tảng đấu giá thời gian thực. Hệ thống cung cấp API RESTful toàn diện để quản lý tài khoản người dùng, số dư quỹ, niêm yết các mặt hàng đấu giá, đặt giá thầu theo thời gian thực và truyền phát (stream) các bản cập nhật giá trực tiếp bằng Server-Sent Events (SSE). Hệ thống được thiết kế để đảm bảo tính mạnh mẽ, bảo mật và khả năng mở rộng, xử lý đặt giá thầu đồng thời và đảm bảo tính nhất quán của dữ liệu thông qua tính toàn vẹn của giao dịch (transactional integrity).

---

## 2. Công nghệ & Môi trường

*   **Framework chính:** Spring Boot
*   **Ngôn ngữ:** Java 26
*   **Cơ sở dữ liệu:** SQLite (nhúng, dễ dàng chuyển đổi nền tảng)
*   **Truy cập Dữ liệu:** Spring Data JPA / Hibernate
*   **Bảo mật:** Spring Security với JWT để xác thực không trạng thái (stateless).
*   **Lập trình Phản ứng (Reactive):** Project Reactor (Flux/Mono) để truyền phát Server-Sent Events (SSE).
*   **Tài liệu API:** Springdoc OpenAPI (Swagger UI)
*   **Công cụ Build:** Gradle

### Yêu cầu Cài đặt

*   **JDK 26:** Đảm bảo bạn đã cài đặt Java Development Kit (JDK) tương thích.
*   **Gradle:** Dự án đã bao gồm sẵn Gradle Wrapper (`gradlew`), vì vậy không bắt buộc phải cài đặt Gradle cục bộ trên máy.

---

## 3. Cấu trúc Dự án

Dự án tuân theo kiến trúc phân lớp tiêu chuẩn, được tổ chức theo tính năng vào các package chính sau:

*   **/auth**: Xử lý đăng ký, đăng nhập và quản lý token người dùng (access token và refresh token).
*   **/users**: Quản lý hồ sơ người dùng và số dư ví.
*   **/items**: Quản lý việc tạo và truy xuất các mặt hàng đấu giá.
*   **/itemstatus**: Theo dõi trạng thái thời gian thực của từng cuộc đấu giá (ví dụ: giá, người trả giá cao nhất, thời gian kết thúc).
*   **/bids**: Chứa logic để đặt và truy xuất các lượt trả giá (bids).
*   **/auctionorchestration**: Dịch vụ cốt lõi phối hợp các tương tác giữa người dùng, mặt hàng và lượt trả giá để đảm bảo tuân thủ các quy tắc nghiệp vụ.
*   **/config**: Chứa cấu hình cấp ứng dụng, bao gồm thiết lập bảo mật và tài liệu Swagger.
*   **/common**: Các thành phần dùng chung, bao gồm các ngoại lệ (exceptions) tùy chỉnh, các lớp bọc phản hồi (response wrappers), và các chú thích (annotations).

---

## 4. Hướng dẫn Chạy

Dự án này sử dụng Gradle wrapper, đảm bảo rằng bất kỳ ai cũng có thể build và chạy dự án với đúng phiên bản Gradle mà không cần cài đặt thủ công. Các câu lệnh có thể chạy trên nhiều nền tảng (Windows, macOS, và Linux).

### Server (Backend)

1.  **Clone repository:**
    ```bash
    git clone https://github.com/your-username/auction-backend.git
    cd auction-backend
    ```

2.  **Cấp quyền thực thi cho Gradle wrapper (chỉ dành cho macOS/Linux):**
    Nếu bạn đang dùng hệ điều hành macOS hoặc Linux, bạn cần cấp quyền thực thi cho script wrapper.
    ```bash
    chmod +x gradlew
    ```

3.  **Chạy ứng dụng:**
    Sử dụng câu lệnh sau để build và khởi động server.
    ```bash
    ./gradlew bootRun
    ```

Backend server sẽ khởi chạy tại `http://localhost:8080`.

### Client (Frontend)

*(Vui lòng thêm hướng dẫn chạy client frontend của bạn tại đây. Ví dụ:)*

1.  Điều hướng vào thư mục dự án client của bạn.
2.  Chạy lệnh `npm install`.
3.  Chạy lệnh `npm start`.

---

## 5. Các Chức năng đã Hoàn thành

*   **Xác thực Người dùng:** Đầy đủ quy trình đăng ký, đăng nhập và refresh token.
*   **Quản lý Số dư:** Người dùng có thể nạp tiền và xem số dư của mình.
*   **Tạo Phiên Đấu giá:** Người dùng có thể niêm yết các mặt hàng để đấu giá với các thông số chi tiết.
*   **Đấu giá Thời gian thực:** Người dùng có thể đặt giá cho các mặt hàng đang hoạt động. Hệ thống xử lý việc khóa quỹ, hoàn tiền khi có người trả giá cao hơn, và ngăn chặn các lượt trả giá không hợp lệ.
*   **Mua Ngay (Buy It Now):** Chức năng cho phép người dùng mua ngay lập tức một mặt hàng.
*   **Hủy Đấu giá:** Người bán có thể hủy phiên đấu giá nếu chưa có lượt trả giá nào được đặt.
*   **Chống bắn tỉa (Anti-Sniping):** Thời gian kết thúc đấu giá sẽ tự động được gia hạn nếu có lượt trả giá được đặt trong những khoảnh khắc cuối cùng.
*   **Cập nhật Giá Trực tiếp:** Một endpoint Server-Sent Events (SSE) sẽ truyền phát các thay đổi giá đến tất cả các client đang xem một mặt hàng.
*   **Tài liệu API:** API được lập tài liệu đầy đủ và có thể truy cập qua Swagger UI.

---

## 6. Báo cáo & Demo

*   **Báo cáo PDF:** [Link đến báo cáo PDF của bạn trên Google Drive]
*   **Video Demo:** [Link đến video demo của bạn trên Google Drive]
