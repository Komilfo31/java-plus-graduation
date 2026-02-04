package interaction.model.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {

    private String created;
    private Integer event;
    private Integer id;
    private Integer requester;
    private String status;

}
