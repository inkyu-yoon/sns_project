package likelion.sns.domain.dto.read;


import com.fasterxml.jackson.annotation.JsonIgnore;
import likelion.sns.domain.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.text.SimpleDateFormat;

@AllArgsConstructor
@Getter
@ToString(of={"id","title","body","userName","createdAt","lastModifiedAt"})
public class PostDetailDto {
    private Long id;
    private String title;
    private String body;
    private String userName;
    private String createdAt;
    private String lastModifiedAt;

    @JsonIgnore
    private String isModified;

    public PostDetailDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.body = post.getBody();
        this.userName = post.getUser().getUserName();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
        if (post.getModifiedAt() != null) {
            this.createdAt = sdf.format(post.getCreatedAt());
        }
        if (post.getModifiedAt() != null) {
            this.lastModifiedAt = sdf.format(post.getModifiedAt());
        }
        if (!createdAt.equals(lastModifiedAt)) {
            this.isModified = "(수정됨)";
        }
    }
}
