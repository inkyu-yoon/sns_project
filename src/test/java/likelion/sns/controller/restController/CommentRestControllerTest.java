package likelion.sns.controller.restController;

import com.google.gson.Gson;
import likelion.sns.Exception.ErrorCode;
import likelion.sns.Exception.SNSAppException;
import likelion.sns.configuration.SecurityConfig;
import likelion.sns.domain.dto.comment.modify.CommentModifyRequestDto;
import likelion.sns.domain.dto.comment.modify.CommentModifyResponseDto;
import likelion.sns.domain.dto.comment.write.CommentWriteRequestDto;
import likelion.sns.domain.dto.comment.write.CommentWriteResponseDto;
import likelion.sns.domain.entity.UserRole;
import likelion.sns.jwt.JwtTokenUtil;
import likelion.sns.service.CommentService;
import likelion.sns.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CommentRestController.class)
@Import(SecurityConfig.class)
class CommentRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    WebApplicationContext wac;
    @Autowired
    Gson gson;
    @MockBean
    UserService userService;

    @MockBean
    CommentService commentService;

    @Value("${jwt.token.secret}")
    String secretKey;

    Long postId = 1L;
    Long commentId = 1L;

    /**
     * SecurityConfig 로 설정한 Filter Chain 적용
     */

    @BeforeEach
    public void setUpMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // 요청자가 ROLE_USER 임을 가정
        when(userService.findRoleByUserName(any())).thenReturn(UserRole.ROLE_USER);
    }

    /**
     * 댓글 리스트 조회 테스트
     */
    @Nested
    @DisplayName("댓글 리스트 조회 테스트")
    class CommentReadTest {

        /**
         * 댓글 리스트 조회 성공 테스트
         */
        @Test
        @DisplayName("댓글 리스트 조회 성공 테스트")
        public void CommentReadListSuccess() throws Exception {

            mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("SUCCESS"));
        }
    }




    /**
     * 댓글 작성 테스트
     */
    @Nested
    @DisplayName("댓글 작성 테스트")
    class CommentWriteTest {

        CommentWriteRequestDto commentWriteRequestDto = new CommentWriteRequestDto("comment");
        String token = JwtTokenUtil.createToken("userName", secretKey);
        String content = gson.toJson(commentWriteRequestDto);

        /**
         * 댓글 작성 성공 테스트
         */
        @Test
        @DisplayName("댓글 작성 성공 테스트")
        public void commentWriteSuccess() throws Exception {

            when(commentService.writeComment(any(), any(), any()))
                    .thenReturn(new CommentWriteResponseDto(1L, "comment", "userName", postId, "time"));


            mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.comment").value("comment"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.userName").value("userName"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.postId").value(postId))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.createdAt").value("time"));
        }

        /**
         * 댓글 작성 실패 (로그인 하지 않은 경우)
         */
        @Test
        @DisplayName("댓글 작성 실패 (로그인 하지 않은 경우)")
        public void commentWriteError1() throws Exception {

            mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("TOKEN_NOT_FOUND"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("토큰이 존재하지 않습니다."));
        }

        /**
         * 댓글 작성 실패 (게시물이 존재하지 않는 경우)
         */
        @Test
        @DisplayName("댓글 작성 실패 (게시물이 존재하지 않는 경우)")
        public void commentWriteError2() throws Exception {

            when(commentService.writeComment(any(), any(), any()))
                    .thenThrow(new SNSAppException(ErrorCode.POST_NOT_FOUND));


            mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("POST_NOT_FOUND"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("해당 포스트가 없습니다."));
        }
    }

    /**
     * 댓글 수정 테스트
     */
    @Nested
    @DisplayName("댓글 수정 테스트")
    class CommentModifyTest {

        CommentModifyRequestDto commentModifyRequestDto = new CommentModifyRequestDto("new comment");
        String token = JwtTokenUtil.createToken("userName", secretKey);
        String content = gson.toJson(commentModifyRequestDto);

        /**
         * 댓글 수정 성공 테스트
         */
        @Test
        @DisplayName("댓글 수정 성공 테스트")
        public void commentModifySuccess() throws Exception {

            when(commentService.getOneComment(any(), any(), any()))
                    .thenReturn(new CommentModifyResponseDto(commentId, "new comment", "userName", postId, "created time", "modified time"));

            mockMvc.perform(put("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.comment").value("new comment"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.userName").value("userName"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.postId").value(postId))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.createdAt").value("created time"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.modifiedAt").value("modified time"));
        }

        /**
         * 댓글 수정 실패 (토큰 인증 실패)
         */

        @Test
        @DisplayName("댓글 수정 실패 (토큰 인증 실패)")
        public void commentModifyError1() throws Exception {

            mockMvc.perform(put("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("TOKEN_NOT_FOUND"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("토큰이 존재하지 않습니다."));
        }

        /**
         * 댓글 수정 실패 (댓글이 존재하지 않을 시)
         */

        @Test
        @DisplayName("댓글 수정 실패 (댓글이 존재하지 않을 시)")
        public void commentModifyError2() throws Exception {

            doThrow(new SNSAppException(ErrorCode.COMMENT_NOT_FOUND))
                    .when(commentService).modifyComment(any(), any(), any(), any());

            mockMvc.perform(put("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("COMMENT_NOT_FOUND"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("해당 댓글이 없습니다."));
        }


        /**
         * 댓글 수정 실패 (작성자와 요청자가 다를 경우)
         */

        @Test
        @DisplayName("댓글 수정 실패 (작성자와 요청자가 다를 경우)")
        public void commentModifyError3() throws Exception {

            doThrow(new SNSAppException(ErrorCode.USER_NOT_MATCH))
                    .when(commentService).modifyComment(any(), any(), any(), any());

            mockMvc.perform(put("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("USER_NOT_MATCH"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("작성자와 요청자가 일치하지 않습니다."));
        }

        /**
         * 댓글 수정 실패 (DB 에러)
         */

        @Test
        @DisplayName("댓글 수정 실패 (DB 에러)")
        public void commentModifyError4() throws Exception {

            doThrow(new SNSAppException(ErrorCode.DATABASE_ERROR))
                    .when(commentService).modifyComment(any(), any(), any(), any());

            mockMvc.perform(put("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .characterEncoding("utf-8")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("DATABASE_ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("DB 에러"));
        }
    }


     /**
     * 댓글 삭제 테스트
     */
    @Nested
    @DisplayName("댓글 삭제 테스트")
    class CommentDeleteTest {

        String token = JwtTokenUtil.createToken("userName", secretKey);

        /**
         * 댓글 삭제 성공 테스트
         */
        @Test
        @DisplayName("댓글 삭제 성공 테스트")
        public void commentDeleteSuccess() throws Exception {

            mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.commentId").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("댓글 삭제 완료"));
        }

        /**
         * 댓글 삭제 실패 (토큰 인증 실패)
         */

        @Test
        @DisplayName("댓글 삭제 실패 (토큰 인증 실패)")
        public void commentDeleteError1() throws Exception {

            mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + commentId))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("TOKEN_NOT_FOUND"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("토큰이 존재하지 않습니다."));
        }

        /**
         * 댓글 삭제 실패 (댓글이 존재하지 않을 시)
         */

        @Test
        @DisplayName("댓글 삭제 실패 (댓글이 존재하지 않을 시)")
        public void commentDeleteError2() throws Exception {

            doThrow(new SNSAppException(ErrorCode.COMMENT_NOT_FOUND))
                    .when(commentService).deleteComment(any(), any(), any());

            mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("COMMENT_NOT_FOUND"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("해당 댓글이 없습니다."));
        }


        /**
         * 댓글 삭제 실패 (작성자와 요청자가 다를 경우)
         */

        @Test
        @DisplayName("댓글 삭제 실패 (작성자와 요청자가 다를 경우)")
        public void commentDeleteError3() throws Exception {

            doThrow(new SNSAppException(ErrorCode.USER_NOT_MATCH))
                    .when(commentService).deleteComment(any(), any(), any());

            mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("USER_NOT_MATCH"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("작성자와 요청자가 일치하지 않습니다."));
        }

        /**
         * 댓글 삭제 실패 (DB 에러)
         */

        @Test
        @DisplayName("댓글 삭제 실패 (DB 에러)")
        public void commentDeleteError4() throws Exception {

            doThrow(new SNSAppException(ErrorCode.DATABASE_ERROR))
                    .when(commentService).deleteComment(any(), any(), any());

            mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + commentId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.errorCode").value("DATABASE_ERROR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.message").value("DB 에러"));
        }
    }



}