package ru.practicum.category.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.validation.FieldDescription;

@Entity
@Table(name = "categories")
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldDescription(value = "Уникальный идентификатор категории", changeByCopy = false)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    @FieldDescription("Наименование категории")
    private String name;
}