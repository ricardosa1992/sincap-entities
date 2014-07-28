package br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.dto;

import java.util.Calendar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.CausaMortis;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.CorpoEncaminhamento;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.TipoObito;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObitoDTO {

    private Long id;
    private PacienteDTO paciente = new PacienteDTO();
    private Calendar dataObito;
    private Calendar dataCadastro;
    private boolean aptoDoacao;
    private CorpoEncaminhamento corpoEncaminhamento;
    private CausaMortis primeiraCausaMortis;
    private CausaMortis segundaCausaMortis;
    private CausaMortis terceiraCausaMortis;
    private CausaMortis quartaCausaMortis;
    private Long setor;
    private Long hospital;
    private TipoObito tipoObito;
}