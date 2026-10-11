package restaurant.site.mapper;


import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Entity → Response DTO
    UserDto.Response toResponse(User user);

    // Request DTO → Entity

    User toEntity(UserDto.Request request);
}
