package ru.practicum.statistics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {

    private Integer id;
    @NotBlank(message = "The service identifier cannot be empty")
    private String app;
    @NotBlank(message = "URI cannot be empty")
    private String uri;
    @NotBlank(message = "User's IP-address cannot be empty ")
    private String ip;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp;

}
