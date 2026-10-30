package dev.nickbern.mercado_paramo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "productores")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class Productor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuario usuario;

    private String vereda;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private EstadoVerificacion estadoVerificacion;
}
