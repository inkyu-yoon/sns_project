package likelion.sns.jwt;

import com.google.gson.Gson;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import likelion.sns.Exception.ErrorCode;
import likelion.sns.Exception.ErrorDto;
import likelion.sns.Exception.SNSAppException;
import likelion.sns.domain.Response;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.NoSuchElementException;

public class ExceptionHandlerFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            //토큰의 유효기간 만료
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            //유효하지 않은 토큰
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
            //사용자 찾을 수 없음
        } catch (NoSuchElementException e){
            setErrorResponse(response, ErrorCode.USERNAME_NOT_FOUND);
        }
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) {
        Gson gson = new Gson();
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getWriter().write(gson.toJson(Response.error(new ErrorDto(new SNSAppException(errorCode)))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
