package likelion.sns.controller;

import likelion.sns.Exception.ErrorCode;
import likelion.sns.Exception.SNSAppException;
import likelion.sns.domain.Response;
import likelion.sns.domain.dto.write.PostWriteRequestDto;
import likelion.sns.domain.dto.read.PostDetailDto;
import likelion.sns.domain.dto.read.PostListDto;
import likelion.sns.domain.dto.write.PostWriteResponseDto;
import likelion.sns.domain.entity.Post;
import likelion.sns.repository.PostRepository;
import likelion.sns.service.PostService;
import likelion.sns.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;

    @GetMapping("")
    public Page<PostListDto> showPostList(Pageable pageable) {
        return postService.getPostList(pageable);
    }

    @GetMapping("/{postId}")
    public PostDetailDto showOne(@PathVariable(name = "postId") Long id) {
        return postService.getPostById(id);
    }

    @PostMapping
    public Response write(@RequestBody PostWriteRequestDto postWriteRequestDto, Authentication authentication) {
        if (authentication == null) {
            throw new SNSAppException(ErrorCode.INVALID_PERMISSION);
        }
        if (!authentication.isAuthenticated()) {
            throw new SNSAppException(ErrorCode.INVALID_TOKEN);
        }
        String userName = authentication.getName();
        PostWriteResponseDto postWriteResponseDto = postService.writePost(postWriteRequestDto, userName);
        return Response.success(postWriteResponseDto);

    }
}
