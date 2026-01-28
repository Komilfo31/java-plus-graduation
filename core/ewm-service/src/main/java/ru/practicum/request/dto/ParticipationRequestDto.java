package ru.practicum.request.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequestDto {

    private String created;
    private Integer event;
    private Integer id;
    private Integer requester;
    private String status;

}
