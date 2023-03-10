package likelion.sns.controller.restController;

import likelion.sns.Exception.ErrorCode;
import likelion.sns.Exception.SNSAppException;
import likelion.sns.configuration.SecurityConfig;
import likelion.sns.domain.entity.UserRole;
import likelion.sns.jwt.JwtTokenUtil;
import likelion.sns.service.LikeService;
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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LikeRestController.class)
@Import(SecurityConfig.class)
class LikeRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    WebApplicationContext wac;
    @MockBean
    UserService userService;

    @MockBean
    LikeService likeService;

    @Value("${jwt.token.secret}")
    String secretKey;

    Long postId = 1L;

    /**
     * SecurityConfig ??? ????????? Filter Chain ??????
     */

    @BeforeEach
    public void setUpMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // ???????????? ROLE_USER ?????? ??????
        when(userService.findRoleByUserName(any())).thenReturn(UserRole.ROLE_USER);
    }

    /**
     * ????????? ?????? ?????????
     */
    @Nested
    @DisplayName("????????? ?????? ?????????")
    class LikeTest {
        String token = JwtTokenUtil.createToken("userName",1L, secretKey);

        /**
         * ????????? ?????? ?????? ?????????
         */
        @Test
        @DisplayName("????????? ?????? ?????? ?????????")
        public void likeSuccess() throws Exception {

            mockMvc.perform(post("/api/v1/posts/" + postId + "/likes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result").value("???????????? ???????????????."));
        }

        /**
         * ????????? ?????? ?????? (????????? ?????? ?????? ??????)
         */

        @Test
        @DisplayName("????????? ?????? ?????? (????????? ?????? ?????? ??????)")
        public void likeError1() throws Exception {

            mockMvc.perform(post("/api/v1/posts/" + postId + "/likes"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("TOKEN_NOT_FOUND"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.message").value("????????? ???????????? ????????????."));
        }

        /**
         * ????????? ?????? ?????? (?????? ???????????? ?????? ??????)
         */

        @Test
        @DisplayName("????????? ?????? ?????? (?????? ???????????? ?????? ??????)")
        public void likeError2() throws Exception {

            doThrow(new SNSAppException(ErrorCode.POST_NOT_FOUND)).when(likeService).AddLike(any(),any());

            mockMvc.perform(post("/api/v1/posts/" + postId + "/likes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("POST_NOT_FOUND"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.message").value("?????? ???????????? ????????????."));
        }

        /**
         * ????????? ?????? ?????? (?????? ???????????? ????????? ??????)
         */

        @Test
        @DisplayName("????????? ?????? ?????? (?????? ???????????? ????????? ??????)")
        public void likeError3() throws Exception {

            doThrow(new SNSAppException(ErrorCode.FORBIDDEN_ADD_LIKE)).when(likeService).AddLike(any(),any());

            mockMvc.perform(post("/api/v1/posts/" + postId + "/likes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("FORBIDDEN_ADD_LIKE"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.message").value("???????????? ????????? ?????? ??? ????????????."));
        }
    }
    /**
     * ????????? ?????? ?????????
     */
    @Nested
    @DisplayName("????????? ?????? ?????????")
    class LikeCountTest {

        /**
         * ????????? ?????? ????????? ?????? ?????????
         */
        @Test
        @DisplayName("????????? ?????? ????????? ?????? ?????????")
        public void likeCountSuccess() throws Exception {

            when(likeService.getLikesCount(postId)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/posts/" + postId + "/likes"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result").value(1));
        }
    }
}