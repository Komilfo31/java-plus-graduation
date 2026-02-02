package interaction.client;

import interaction.model.event.dto.EventFullDto;
import interaction.model.event.dto.EventShortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "event-service", path = "/events")
public interface EventFeignClient {

    @GetMapping("/{id}/status")
    EventFullDto getEventStatus(@PathVariable("id") Integer eventId);

    @GetMapping("/{id}")
    EventShortDto getEventById(@PathVariable("id") Integer eventId);

    @GetMapping("/full-event-by-id")
    EventFullDto getEventFullDtoById(@RequestParam("eventId") Integer eventId);

    @GetMapping("/short-event-by-id")
    EventShortDto getEventShortDtoById(@RequestParam("eventId") Integer eventId);

    @GetMapping("/by-ids")
    List<EventShortDto> getByIds(@RequestParam("eventIds") List<Integer> eventIds);

    @GetMapping("/exists/{id}")
    Boolean existsById(@PathVariable("id") Integer eventId);

    @GetMapping("/{id}/state")
    String getEventState(@PathVariable("id") Integer eventId);
}
