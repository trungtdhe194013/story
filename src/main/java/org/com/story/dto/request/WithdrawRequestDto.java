package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WithdrawRequestDto {

    @NotNull(message = "Amount is required")
    @Min(value = 50000, message = "Số coin rút tối thiểu là 50,000")
    private Long amount;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    private String bankAccount;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    private String bankOwner;

    private String note;
}
