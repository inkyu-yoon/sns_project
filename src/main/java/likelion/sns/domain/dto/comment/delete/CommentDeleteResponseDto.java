package likelion.sns.domain.dto.comment.delete;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString(of={"message","id"})
public class CommentDeleteResponseDto {
    private String message;
    private Long id;

    public CommentDeleteResponseDto(Long commentId) {
        this.message = "댓글 삭제 완료";
        this.id = commentId;
    }
}
