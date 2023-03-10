package likelion.sns.domain.dto.post.delete;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString(of={"message","postId"})
public class PostDeleteResponseDto {
    private String message;
    private Long postId;

    public PostDeleteResponseDto(Long postId) {
        this.message = "포스트 삭제 완료";
        this.postId = postId;
    }
}
