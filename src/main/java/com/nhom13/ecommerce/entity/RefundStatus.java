package com.nhom13.ecommerce.entity;

/**
 * Định nghĩa các trạng thái của một yêu cầu trả hàng.
 * Tên enum là số ít (RefundStatus) theo quy ước.[2, 3]
 */
public enum RefundStatus {
    /**
     * Yêu cầu vừa được tạo bởi khách hàng, đang chờ Admin xem xét.
     */
    PENDING,

    /**
     * Yêu cầu đã bị Admin từ chối. (Trạng thái kết thúc)
     * Khi bị từ chối, số lượng hàng "đang chờ trả" sẽ được hoàn lại cho OrderItem.
     */
    REJECTED,

    /**
     * Yêu cầu đã được Admin phê duyệt và đang trong quá trình xử lý
     * (ví dụ: chờ nhận hàng, kiểm tra hàng).
     */
    PROCESSING,

    /**
     * Yêu cầu đã được xử lý hoàn tất, tiền đã hoàn, hàng đã nhập kho. (Trạng thái kết thúc)
     * Khi hoàn tất, số lượng hàng sẽ được cộng trở lại vào kho (Product.stockQuantity).
     */
    COMPLETED
}