package likelion.sns.controller.restController;

import com.google.gson.Gson;
import likelion.sns.Exception.ErrorCode;
import likelion.sns.Exception.ErrorDto;
import likelion.sns.Exception.ExceptionManager;
import likelion.sns.Exception.SNSAppException;
import likelion.sns.configuration.SecurityConfig;
import likelion.sns.domain.Response;
import likelion.sns.domain.dto.user.changeRole.UserRoleChangeRequestDto;
import likelion.sns.domain.dto.user.join.UserJoinRequestDto;
import likelion.sns.domain.dto.user.join.UserJoinResponseDto;
import likelion.sns.domain.dto.user.login.UserLoginRequestDto;
import likelion.sns.domain.dto.user.login.UserLoginResponseDto;
import likelion.sns.domain.entity.UserRole;
import likelion.sns.jwt.JwtTokenUtil;
import likelion.sns.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserRestController.class)
@Import({SecurityConfig.class})
class UserRestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired WebApplicationContext wac;
    @Autowired Gson gson;

    @Value("${jwt.token.secret}")
    String secretKey;
    @MockBean UserService userService;

    @Mock
    BindingResult br;

    UserJoinRequestDto userJoinRequestDto = new UserJoinRequestDto("?????????", "password");
    UserLoginRequestDto userLoginRequestDto = new UserLoginRequestDto("?????????", "1234");


    /**
     * SecurityConfig ??? ????????? Filter Chain ??????
     */

    @BeforeEach
    public void setUpMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // ?????? ?????? ?????? ?????????

    }



    /**
     * ???????????? ?????????
     */

    @Nested
    @DisplayName("???????????? ?????????")
    public class JoinTest {
        String content = gson.toJson(userJoinRequestDto);

        /**
         * ???????????? ?????? ?????????
         */
        @Test
        @DisplayName("???????????? (join) ?????? ?????????")
        void joinSuccess() throws Exception {

            given(userService.createUser(any()))
                    .willReturn(new UserJoinResponseDto(1L, userLoginRequestDto.getUserName()));

            mockMvc.perform(post("/api/v1/users/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.userId").value(1))
                    .andExpect(jsonPath("$.result.userName").value(userLoginRequestDto.getUserName()));
        }

        /**
         * ???????????? ?????? ????????? (?????? ????????? ???????????? ??????)
         */

        @Test
        @DisplayName("???????????? ?????? ?????? ?????????")
        void joinError1() throws Exception {

            when(userService.createUser(any()))
                    .thenThrow(new SNSAppException(ErrorCode.DUPLICATED_USER_NAME));

            mockMvc.perform(post("/api/v1/users/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("utf-8")
                            .content(content))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("DUPLICATED_USER_NAME"))
                    .andExpect(jsonPath("$.result.message").value("UserName??? ???????????????."));
        }

        /**
         * ???????????? ?????? ????????? (????????? ?????? ??????)
         */

        @Test
        @DisplayName("???????????? ?????? ????????? (????????? ?????? ??????)")
        void joinError3() throws Exception {

            MockedStatic<ExceptionManager> exceptionManagerMockedStatic = mockStatic(ExceptionManager.class);
            ErrorCode e = ErrorCode.BLANK_NOT_ALLOWED;
            UserJoinRequestDto requestDto = new UserJoinRequestDto(null, null);
            String content = gson.toJson(requestDto);

            when(br.hasErrors())
                    .thenReturn(true);

            when(ExceptionManager.ifNullAndBlank())
                    .thenReturn(ResponseEntity.status(e.getHttpStatus()).body(Response.error(new ErrorDto(e))));

            when(userService.createUser(any()))
                    .thenThrow(new SNSAppException(e));

            mockMvc.perform(post("/api/v1/users/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("utf-8")
                            .content(content))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("BLANK_NOT_ALLOWED"))
                    .andExpect(jsonPath("$.result.message").value("?????? ?????? null ??? ????????? ??? ????????????."));

            exceptionManagerMockedStatic.close();

        }

    }

    /**
     * ????????? ?????????
     */

    @Nested
    @DisplayName("????????? ?????????")
    public class LoginTest {

        String content = gson.toJson(userLoginRequestDto);

        @Mock
        UserLoginResponseDto responseDto;
        /**
         * ????????? ?????? ?????????
         */
        @Test
        @DisplayName("?????? ????????? ?????? ?????????")
        void loginSuccess() throws Exception {

            String token = "randomToken";

            given(userService.loginUser(any()))
                    .willReturn(new UserLoginResponseDto(token));

            mockMvc.perform(post("/api/v1/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andDo(print())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(jsonPath("$.result.jwt").exists())
                    .andExpect(jsonPath("$.result.jwt").value(token));

        }



        /**
         * ????????? ?????? ????????? (???????????? ?????? UserName ??????)
         */
        @Test
        @DisplayName("?????? ????????? ?????? ????????? (???????????? ?????? UserName ??????)")
        void LoginNotFound() throws Exception {

            when(userService.loginUser(any()))
                    .thenThrow(new SNSAppException(ErrorCode.USERNAME_NOT_FOUND));

            mockMvc.perform(post("/api/v1/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("USERNAME_NOT_FOUND"))
                    .andExpect(jsonPath("$.result.message").value("???????????? ????????? ?????? ??? ????????????."));

        }

        /**
         * ????????? ?????? ????????? (??????????????? ???????????? ??????)
         */
        @Test
        @DisplayName("?????? ????????? ?????? ????????? (Invalid password)")
        void LoginInValid() throws Exception {

            when(userService.loginUser(any()))
                    .thenThrow(new SNSAppException(ErrorCode.INVALID_PASSWORD));

            mockMvc.perform(post("/api/v1/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("INVALID_PASSWORD"))
                    .andExpect(jsonPath("$.result.message").value("??????????????? ?????????????????????."));

        }
        /**
         * ????????? ?????? ????????? (????????? ?????? ??????)
         */

        @Test
        @DisplayName("????????? ?????? ????????? (????????? ?????? ??????)")
        void loginError3() throws Exception {

            MockedStatic<ExceptionManager> exceptionManagerMockedStatic = mockStatic(ExceptionManager.class);
            ErrorCode e = ErrorCode.BLANK_NOT_ALLOWED;
            UserLoginRequestDto requestDto = new UserLoginRequestDto(null, null);
            String content = gson.toJson(requestDto);

            when(br.hasErrors())
                    .thenReturn(true);

            when(ExceptionManager.ifNullAndBlank())
                    .thenReturn(ResponseEntity.status(e.getHttpStatus()).body(Response.error(new ErrorDto(e))));

            when(userService.loginUser(any()))
                    .thenThrow(new SNSAppException(e));

            mockMvc.perform(post("/api/v1/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("utf-8")
                            .content(content))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("BLANK_NOT_ALLOWED"))
                    .andExpect(jsonPath("$.result.message").value("?????? ?????? null ??? ????????? ??? ????????????."));

            exceptionManagerMockedStatic.close();

        }

    }


    /**
     *  ????????? ?????? ?????? ?????? ?????? ?????????
     */

    @Nested
    @DisplayName("???????????? ?????????")
    public class ChangeRoleTest {

        UserRoleChangeRequestDto userRoleChangeRequestDto = new UserRoleChangeRequestDto("ADMIN");
        String content = gson.toJson(userRoleChangeRequestDto);

        String token = JwtTokenUtil.createToken("userName",1L, secretKey);


        /**
         * ?????? ?????? ?????? ?????????
         */
        @Test
        @DisplayName("?????? ?????? ?????? ?????????")
        void changeRoleSuccess() throws Exception {

            when(userService.findRoleByUserName(any())).thenReturn(UserRole.ROLE_ADMIN);

            mockMvc.perform(post("/api/v1/users/1/role/change")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andDo(print())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                    .andExpect(jsonPath("$.result.userId").exists())
                    .andExpect(jsonPath("$.result.userId").value(1))
                    .andExpect(jsonPath("$.result.message").exists())
                    .andExpect(jsonPath("$.result.message").value("1??? ???????????? ????????? ?????????????????????."));

        }

        /**
         * ?????? ?????? ?????? ????????? ( USER ????????? ????????? ???????????? ??????)
         */
        @Test
        @DisplayName("?????? ?????? ?????? ????????? ( USER ????????? ????????? ???????????? ??????)")
        void changeRoleError1() throws Exception {

            when(userService.findRoleByUserName(any())).thenReturn(UserRole.ROLE_USER);

            mockMvc.perform(post("/api/v1/users/1/role/change")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andDo(print())
                    .andExpect(jsonPath("$.resultCode").exists())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result.errorCode").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("FORBIDDEN_REQUEST"))
                    .andExpect(jsonPath("$.result.message").exists())
                    .andExpect(jsonPath("$.result.message").value("ADMIN ????????? ????????? ??? ????????????."));

        }


        /**
         * ?????? ?????? ?????? ????????? (?????? ?????? ???????????? ??????)
         */
        @Test
        @DisplayName("?????? ????????? ?????? ????????? (???????????? ?????? UserName ??????)")
        void changeRoleError2() throws Exception {

            mockMvc.perform(post("/api/v1/users/1/role/change")
                            .content(content)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultCode").value("ERROR"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.errorCode").value("TOKEN_NOT_FOUND"))
                    .andExpect(jsonPath("$.result.message").value("????????? ???????????? ????????????."));
        }

    }
}