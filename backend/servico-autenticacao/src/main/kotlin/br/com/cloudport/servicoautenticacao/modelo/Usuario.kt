package br.com.cloudport.servicoautenticacao.modelo

import javax.persistence.*
import java.time.LocalDate

@Entity
data class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val cpf: String,
    val nome: String,
    val email: String,
    val cnpj: String,
    val razaoSocial: String,
    val perfilAcesso: String,

    @Enumerated(EnumType.STRING)
    val statusCadastro: StatusCadastro,

    val usuarioAprovacao: String,
    val dataAprovacao: LocalDate,
    val dataCadastro: LocalDate
)
