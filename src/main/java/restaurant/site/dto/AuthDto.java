package restaurant.site.dto;


import lombok.*;
import restaurant.site.entity.User.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

   @Data
    public static class Request {

        @NotBlank(message = "user.email.required")
        @Email(message = "user.email.invalid")
        private String email;

        // BCrypt only uses the first 72 bytes; refuse longer input instead of hashing it
        @NotBlank(message = "user.password.required")
        @Size(max = 72, message = "user.password.size")
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
