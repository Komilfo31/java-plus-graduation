package interaction.client;

import interaction.model.user.UserShortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    UserShortDto getById(@PathVariable("id") Integer id);

    @GetMapping("/by-ids")
    List<UserShortDto> getByIds(@RequestParam List<Integer> userIds);
}
