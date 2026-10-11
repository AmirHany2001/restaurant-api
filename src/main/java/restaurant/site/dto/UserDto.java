package restaurant.site.dto;


import jakarta.validation.constraints.*;
import lombok.*;
import restaurant.site.entity.User.Role;


public class UserDto {

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "user.fullName.required")
        @Size(min = 3, max = 100, message = "user.fullName.size")
        private String fullName;

        @NotEmpty(message = "user.email.required")
        @Email(message = "user.email.invalid")
        private String email;

        @NotEmpty(message = "user.password.required")
        @Size(min = 8, message = "user.password.size")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).*$",
                message = "user.password.invalid"
        )
        private String password;

        @NotEmpty(message = "user.phone.required")
        @Pattern(regexp = "^01[0-9]{9}$", message = "user.phone.invalid")
        private String phoneNumber;


        @NotBlank(message = "user.address.required")
        @Size(min = 5, message = "user.address.size")
        private String address;

    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String address;
        private Role role;
        // No password — never send it back to the client
    }
}