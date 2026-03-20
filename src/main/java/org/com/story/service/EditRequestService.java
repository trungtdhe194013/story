package org.com.story.service;

import org.com.story.dto.request.CreateEditRequestDto;
import org.com.story.dto.request.RejectEditDto;
import org.com.story.dto.request.SubmitEditDto;
import org.com.story.dto.response.EditRequestResponse;

import java.util.List;

public interface EditRequestService {

    // ============ AUTHOR SIDE ============

    /** Author tạo yêu cầu chỉnh sửa + lock coin */
    EditRequestResponse createRequest(CreateEditRequestDto dto);

    /** Author xem tất cả requests của mình */
    List<EditRequestResponse> getMyRequestsAsAuthor();

    /** Author chấp thuận bản edit → coin chuyển cho Editor, chapter.content cập nhật */
    EditRequestResponse approveEdit(Long requestId);

    /**
     * Author từ chối bản edit → Editor có thể viết lại (IN_PROGRESS).
     * Coin vẫn bị lock, chưa hoàn trả.
     */
    EditRequestResponse rejectEdit(Long requestId, RejectEditDto dto);

    /** Author huỷ request (chỉ khi OPEN) → coin hoàn lại */
    EditRequestResponse cancelRequest(Long requestId);

    // ============ EDITOR SIDE ============

    /** Editor xem danh sách yêu cầu đang OPEN */
    List<EditRequestResponse> getOpenRequests();

    /** Editor nhận việc (OPEN → IN_PROGRESS) */
    EditRequestResponse assignRequest(Long requestId);

    /** Editor nộp bản chỉnh sửa (IN_PROGRESS → SUBMITTED) */
    EditRequestResponse submitEdit(Long requestId, SubmitEditDto dto);

    /** Editor bỏ việc (IN_PROGRESS → OPEN), chỉ được khi chưa submit lần nào */
    EditRequestResponse withdrawFromRequest(Long requestId);

    /** Editor xem danh sách việc đã nhận/đang làm */
    List<EditRequestResponse> getMyRequestsAsEditor();
}

