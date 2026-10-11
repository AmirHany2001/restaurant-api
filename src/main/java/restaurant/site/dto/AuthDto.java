package restaurant.site.dto;


import lombok.*;
import restaurant.site.entity.User.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDto {

   @Data
    public static class Request {

        @NotBlank(message = "user.email.required")
        @Email(message = "user.email.invalid")
        private String email;

        @NotBlank(message = "user.password.required")
        private String password;
    }

    @Data
    public static class Response {

        private String token;
        private String tokenType = "Bearer";
        private Long userId;
        private String fullName;
        private String email;
        private Role role;
    }
}
