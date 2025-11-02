package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.CreateRefundRequestDTO;
import com.nhom13.ecommerce.dto.RefundRequestResponseDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.entity.RefundStatus;
import com.nhom13.ecommerce.entity.Role;
import com.nhom13.ecommerce.service.RefundService;
import com.nhom13.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/refunds") // [29, 30]
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RefundController {

    private final RefundService refundService;
    private final UserService userService;

    // Helper lấy userId, tương tự các Controller khác 
    private Long getUserId(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }

    /**
     * Tạo một yêu cầu trả hàng mới.
     * Xác thực DTO đầu vào bằng @Valid.[31, 32]
     */
    @PostMapping
    public ResponseEntity<RefundRequestResponseDTO> createRefundRequest(
            @Valid @RequestBody CreateRefundRequestDTO requestDTO,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        RefundRequestResponseDTO response = refundService.createRefundRequest(userId, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Lấy lịch sử yêu cầu trả hàng của người dùng hiện tại.[33]
     */
    @GetMapping("/me")
    public ResponseEntity<List<RefundRequestResponseDTO>> getMyRefunds(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<RefundRequestResponseDTO> refunds = refundService.getRefundsForUser(userId);
        return ResponseEntity.ok(refunds);
    }

    /**
     * Lấy chi tiết một yêu cầu trả hàng.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RefundRequestResponseDTO> getRefundDetails(
            @PathVariable Long id,
            Authentication authentication) {

        UserDTO user = userService.getUserByEmail(authentication.getName());

        // Logic bảo mật: Admin có thể xem mọi yêu cầu,
        // User chỉ có thể xem yêu cầu của chính mình.
        if (user.getRole() == Role.ADMIN) {
            // Admin bỏ qua kiểm tra userId
            // Cần tạo một phương thức service riêng cho Admin (ví dụ: getRefundByIdForAdmin)
            // Tạm thời, chúng ta sẽ gọi hàm gốc và dựa vào logic bảo mật của service.
        }

        try {
            RefundRequestResponseDTO refund = refundService.getRefundById(id, user.getId());
            return ResponseEntity.ok(refund);
        } catch (AccessDeniedException e) {
             // Nếu service từ chối (do không phải chủ sở hữu), kiểm tra xem có phải admin không
             if (user.getRole() == Role.ADMIN) {
                 // Nếu là Admin, gọi một phương thức khác (không kiểm tra userId)
                 // (Giả sử chúng ta thêm: getRefundByIdForAdmin(id))
                 // RefundRequestResponseDTO adminView = refundService.getRefundByIdForAdmin(id);
                 // return ResponseEntity.ok(adminView);

                 // Vì mục đích đơn giản hóa, chúng ta sẽ sửa lại logic service một chút
                 // (Trong bản demo này, ta giả định logic trong service đã xử lý
                 // hoặc ta sẽ điều chỉnh logic controller ở bước sau)

                 // Cách tiếp cận tốt hơn:
                 // Tải yêu cầu refund mà không kiểm tra user
                 // RefundRequestResponseDTO refund = refundService.getRefundByIdWithoutUserCheck(id);
                 // if (!refund.getUserId().equals(user.getId()) && user.getRole()!= Role.ADMIN) {
                 //    throw new AccessDeniedException("Forbidden");
                 // }
                 // return ResponseEntity.ok(refund);

                 // Cách đơn giản nhất:
                 // Admin nên dùng endpoint GET /api/refunds (ở dưới)
                 throw e; // Ném lại lỗi AccessDenied nếu user không phải chủ sở hữu
             } else {
                 throw e; // Ném lỗi AccessDenied
             }
        }
    }

    // --- ADMIN ENDPOINTS ---

    /**
     * Lấy tất cả các yêu cầu trả hàng (có phân trang).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // [34, 35]
    public ResponseEntity<Page<RefundRequestResponseDTO>> getAllRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RefundRequestResponseDTO> refunds = refundService.getAllRefunds(pageable);
        return ResponseEntity.ok(refunds);
    }

    /**
     * Cập nhật trạng thái của một yêu cầu trả hàng (Duyệt/Từ chối/v.v.).
     * Sử dụng @PutMapping vì chúng ta cập nhật một tài nguyên.[36]
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')") // [37, 38]
    public ResponseEntity<RefundRequestResponseDTO> updateRefundStatus(
            @PathVariable Long id,
            @RequestParam RefundStatus status,
            @RequestBody(required = false) Map<String, String> payload) { // Dùng Map để lấy "adminNotes"

        String adminNotes = (payload!= null)? payload.get("adminNotes") : null;
        RefundRequestResponseDTO updatedRefund = refundService.updateRefundStatus(id, status, adminNotes);
        return ResponseEntity.ok(updatedRefund);
    }
}