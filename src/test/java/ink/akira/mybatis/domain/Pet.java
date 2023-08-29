package ink.akira.mybatis.domain;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Pet {
    private Long id;
    private String petName;
    private int age;
}
