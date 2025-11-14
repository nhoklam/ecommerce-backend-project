# Project Backend E-commerce (Nhóm 13)

Đây là project backend cho một ứng dụng thương mại điện tử, được xây dựng bằng Spring Boot. Project bao gồm các tính năng xác thực, quản lý sản phẩm, giỏ hàng, đặt hàng, thanh toán (VNPAY), và nhiều tính năng khác.

## ✨ Tính năng nổi bật

* **Xác thực & Phân quyền:** Đăng ký, đăng nhập , đăng xuất , quên mật khẩu , đổi mật khẩu (sử dụng JWT). Phân quyền Admin và Customer .
* **Quản lý Sản phẩm & Danh mục:** CRUD cho Sản phẩm và Danh mục , tìm kiếm và lọc sản phẩm .
* **Giỏ hàng (Cart):** Thêm , xóa , cập nhật giỏ hàng .
* **Quản lý Đơn hàng (Order):** Tạo đơn hàng (hỗ trợ COD và VNPAY) , xem lịch sử đơn hàng , hủy đơn hàng . Admin có thể cập nhật trạng thái đơn hàng .
* **Thanh toán Online:** Tích hợp cổng thanh toán VNPAY (tạo URL thanh toán , xử lý IPN và Return URL ).
* **Quản lý Hoàn hàng (Refund):** Người dùng có thể yêu cầu hoàn hàng cho các đơn đã giao , Admin có thể duyệt hoặc từ chối .
* **Đánh giá (Review):** Người dùng có thể đánh giá sản phẩm đã mua (kèm ảnh) , xem đánh giá .
* **Tính năng người dùng:** Quản lý thông tin cá nhân , địa chỉ , danh sách yêu thích (Wishlist) , lịch sử tìm kiếm .
* **Tính năng Admin:** Thống kê doanh thu , quản lý banner quảng cáo , quản lý người dùng .

## 🛠️ Công nghệ sử dụng

* **Ngôn ngữ:** Java 21 
* **Framework:** Spring Boot 3.3.4 
* **Bảo mật:** Spring Security (JWT) 
* **Cơ sở dữ liệu:** Spring Data JPA (Hibernate) 
* **Database:** MySQL 
* **API Docs:** Springdoc (Swagger-UI) 
* **Build:** Maven
* **Khác:** Lombok, Spring Mail, v.v.

## 🚀 Cài đặt và Chạy (Local)

### Yêu cầu
* JDK 21 
* Maven 3.8+
* MySQL Server (hoặc một CSDL tương thích)

### Các bước cài đặt
1.  **Clone repository:**
    ```bash
    git clone [https://github.com/nhoklam/ecommerce-backend-project.git](https://github.com/nhoklam/ecommerce-backend-project.git)
    cd ecommerce-backend-project
    ```

2.  **Tạo Cơ sở dữ liệu:**
    * Mở MySQL Workbench hoặc psql.
    * Tạo một database mới với tên `ecommerce_db` (dựa trên `application.yml` ).
    ```sql
    CREATE DATABASE ecommerce_db;
    ```

3.  **Cấu hình Môi trường (`application-dev.yml`):**
    * Mở file `src/main/resources/application-dev.yml` .
    * Cập nhật các thông tin sau cho phù hợp với máy của bạn:

    ```yaml
    spring:
      datasource:
        # Cập nhật password CSDL của bạn tại đây
        password: nhoklam271 
      mail:
        # Cấu hình email (dùng để gửi link reset pass, v.v.)
        # Bạn nên tạo "Mật khẩu ứng dụng" (App Password) cho Gmail
        username: "nhoklam2712005@gmail.com" # <--- Email thật của bạn
        password: "reljmhgynkctdguj"      # <--- Mật khẩu ứng dụng
    
    vnpay:
      # Lấy thông tin này từ tài khoản VNPAY Sandbox
      tmnCode: "YOUR_SANDBOX_TMNCODE"
      hashSecret: "YOUR_SANDBOX_HASHSECRET"
    ```

4.  **Chạy ứng dụng:**
    * Chúng ta sẽ kích hoạt profile `dev` để chạy. Profile này sẽ tự động chạy `DataSeeder` .
    ```bash
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ```
    * (Nếu `mvnw.cmd` bị lỗi, hãy đảm bảo bạn có Maven cài đặt toàn cục và chạy: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`)

5.  **Chạy thành công:**
    * Ứng dụng sẽ chạy tại `http://localhost:8080`.

## 🧪 Hướng dẫn Thử nghiệm (Seed Data)

Khi bạn chạy với profile `dev`, file `DataSeeder.java` sẽ tự động được thực thi và tạo ra các dữ liệu mẫu:

* **Tài khoản Admin:**
    * Email: `admin@ecommerce.com` 
    * Password: `admin123` 
* **Tài khoản Khách hàng:**
    * Email: `customer@ecommerce.com` 
    * Password: `customer123` 

Ngoài ra, seeder cũng tạo sẵn:
* Các danh mục (Laptop, PC, v.v.) 
* Một số sản phẩm (Macbook, Dell, AKKO, v.v.) 
* Địa chỉ, wishlist, và một đơn hàng đã giao (để test review) cho `customer` .

## 📚 API Documentation (Swagger)

Sau khi chạy ứng dụng, tài liệu API (Swagger UI) sẽ có sẵn tại:

**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

Bạn có thể sử dụng giao diện này để thử nghiệm tất cả các API, bao gồm cả các API yêu cầu xác thực (hãy dùng nút "Authorize" sau khi đăng nhập).