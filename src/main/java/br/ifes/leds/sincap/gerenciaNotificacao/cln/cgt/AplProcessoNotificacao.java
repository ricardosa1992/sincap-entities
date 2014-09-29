package br.ifes.leds.sincap.gerenciaNotificacao.cln.cgt;

import br.ifes.leds.reuse.utility.Utility;
import br.ifes.leds.sincap.controleInterno.cln.cdp.Funcionario;
import br.ifes.leds.sincap.controleInterno.cln.cdp.Notificador;
import br.ifes.leds.sincap.gerenciaNotificacao.cgd.ProcessoNotificacaoRepository;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.AtualizacaoEstado;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.Entrevista;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.EstadoNotificacaoEnum;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.ProcessoNotificacao;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.dto.CaptacaoDTO;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.dto.EntrevistaDTO;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.dto.ProcessoNotificacaoDTO;
import org.dozer.Mapper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AplProcessoNotificacao.java
 *
 * @author 20091BSI0273 Classe de servico para regras de negocio da notificacao
 */
@Service
public class AplProcessoNotificacao {

    @Autowired
    private ProcessoNotificacaoRepository notificacaoRepository;
    @Qualifier("mapper")
    @Autowired
    private Mapper mapper;
    @Autowired
    private Utility utility;

    /**
     * Metodo que salva uma nova notificação contendo notificacao de obito
     *
     * @param processoNotificacaoDTO - ProcessoNotificacao - Notificacao que
     *                               sera salva
     * @param idFuncionario          - Id do funcionario que criou a notificacao
     * @return long - Retorna o id do ProcessoNotificacao salvo
     */
    public long salvarNovaNotificacao(ProcessoNotificacaoDTO processoNotificacaoDTO, Long idFuncionario) {

        ProcessoNotificacao notificacao = instanciarNovoProcessoNotificacao(processoNotificacaoDTO);

        notificacao.setNotificador(criarNotificador(idFuncionario));

        setDatasNovaNotificacao(notificacao);

        this.addEstadoInicial(notificacao, idFuncionario);

        notificacao.setCodigo(notificacao.getObito().getPaciente().getNumeroSUS());

        notificacaoRepository.save(notificacao);
        return notificacao.getId();
    }

    private void setDatasNovaNotificacao(ProcessoNotificacao notificacao) {
        if (notificacao.getDataAbertura() == null) {
            notificacao.setDataAbertura(Calendar.getInstance());
        }

        if (notificacao.getObito().getDataCadastro() == null) {
            notificacao.getObito().setDataCadastro(Calendar.getInstance());
        }
    }

    private ProcessoNotificacao instanciarNovoProcessoNotificacao(ProcessoNotificacaoDTO processoNotificacao) {
        ProcessoNotificacao notificacao;
        ProcessoNotificacao notificacaoDTO = mapper.map(processoNotificacao, ProcessoNotificacao.class);

        if (notificacaoDTO.getId() == null) {
            notificacao = new ProcessoNotificacao();
        } else {
            notificacao = notificacaoRepository.findOne(notificacaoDTO.getId());
        }

        notificacao.setAtivo(true);
        notificacao.setObito(notificacaoDTO.getObito());
        notificacao.setCausaNaoDoacao(notificacaoDTO.getCausaNaoDoacao());

        return notificacao;
    }

    private Notificador criarNotificador(Long idFuncionario) {
        Notificador notificador = new Notificador();
        notificador.setId(idFuncionario);
        return notificador;
    }

    /**
     * Metodo que salva uma nova entrevista vinculada a um Processo de
     * Notificacao
     */
    public long salvarEntrevista(Long idProcesso, EntrevistaDTO entrevistaDTO, Long idFuncionario) {

        ProcessoNotificacao notificacao = notificacaoRepository.findOne(idProcesso);
        notificacao.setEntrevista(mapper.map(entrevistaDTO, Entrevista.class));

        if (notificacao.getEntrevista().getDataCadastro() == null) {
            notificacao.getEntrevista().setDataCadastro(Calendar.getInstance());
        }

        this.addNovoEstado(EstadoNotificacaoEnum.AGUARDANDOANALISEENTREVISTA, notificacao, idFuncionario);

        notificacaoRepository.save(notificacao);

        return notificacao.getId();
    }

    /**
     * Metodo que salva um nova captacao vinculada a um Processo de Notificacao
     *
     * @param captacaoDTO - CaptacaoDTO que sera salvo
     * @param idProcesso  - Id do processo de notificacao
     * @param idCaptador  - Id do captador vinculado a notificacao
     */
    public long salvarCaptacao(Long idProcesso, CaptacaoDTO captacaoDTO, Long idCaptador) {

        captacaoDTO.setCaptador(idCaptador);
        ProcessoNotificacaoDTO processo = this.obter(idProcesso);
        processo.setCaptacao(captacaoDTO);

        ProcessoNotificacao notificacao = mapearProcessoNotificacaoDTO(processo);

        this.addNovoEstado(EstadoNotificacaoEnum.AGUARDANDOANALISECAPTACAO, notificacao, idCaptador);

        notificacao.getCaptacao().setDataCadastro((new DateTime()).toCalendar(Locale.getDefault()));

        notificacaoRepository.save(notificacao);

        return notificacao.getId();
    }

    /**
     * Quando um processo de notificacao na etapa obito
     * entra para ser analisado,
     * o mesmo troca o seu estado para EMANALISEOBITO
     */
    public long entrarAnaliseObito(ProcessoNotificacaoDTO processoNotificacaoDTO, Long idFuncionario) {

        return this.addNovoEstadoNoProcessoNotificacao(processoNotificacaoDTO, EstadoNotificacaoEnum.EMANALISEOBITO, idFuncionario);
    }

    /**
     * Quando um processo de notificacao esta em analisa uma das opcoes eh
     * recusar essa analise, dado a erros que haja na notificacao, por exemplo;
     * Voltar para o estado AGUARDANDOANALISEOBITO.
     */
    public Long recusarAnaliseObito(ProcessoNotificacaoDTO processoNotificacaoDTO, Long idFuncionario) {

        return this.addNovoEstadoNoProcessoNotificacao(processoNotificacaoDTO, EstadoNotificacaoEnum.AGUARDANDOCORRECAOOBITO, idFuncionario);
    }

    /**
     * Quando um processo de notificacao etapa obito esta em analisa
     * uma das opcoes eh aceitar essa analise,
     * logo, o estado muda para AGUARDANDOENTREVISTA.
     */
    public Long validarAnaliseObito(ProcessoNotificacaoDTO processoNotificacaoDTO, Long idFuncionario) {
        Long situacao;

        if (processoNotificacaoDTO.getCausaNaoDoacao() == null) {
            situacao = this.addNovoEstadoNoProcessoNotificacao(
                    processoNotificacaoDTO,
                    EstadoNotificacaoEnum.AGUARDANDOENTREVISTA,
                    idFuncionario);
        } else {
            situacao = this.addNovoEstadoNoProcessoNotificacao(
                    processoNotificacaoDTO,
                    EstadoNotificacaoEnum.AGUARDANDOARQUIVAMENTO,
                    idFuncionario);
        }

        return situacao;
    }

    /**
     * Quando um processo de notificacao na etapa entrevista
     * entra para ser analisado,
     * o mesmo troca o seu estado para EMANALISEENTREVISTA
     */
    @SuppressWarnings("unused")
    public Long entrarAnaliseEntrevista(ProcessoNotificacaoDTO processoNotificacaoDTO, Long idFuncionario) {

        return this.addNovoEstadoNoProcessoNotificacao(processoNotificacaoDTO, EstadoNotificacaoEnum.EMANALISEENTREVISTA, idFuncionario);
    }

    /**
     * Quando um processo de notificacao etapa entrevista esta em analisa
     * uma das opcoes eh recusar essa analise,
     * dado a erros que haja na notificacao, por exemplo;
     * Voltar para o estado AGUARDANDOENTREVISTA.
     */
    public Long recusarAnaliseEntrevista(Long idProcessoNotificacao, Long idFuncionario) {

        ProcessoNotificacaoDTO processoNotificacao = obter(idProcessoNotificacao);

        return this.addNovoEstadoNoProcessoNotificacao(processoNotificacao, EstadoNotificacaoEnum.AGUARDANDOCORRECAOENTREVISTA, idFuncionario);
    }

    /**
     * Quando um processo de notificacao etapa entrevsista esta em analisa
     * uma das opcoes eh aceitar essa analise,
     * logo, o estado muda para AGUARDANDOCAPTACAO.
     */
    public Long validarAnaliseEntrevista(Long idProcessoNotificacao,
                                         Long idFuncionario) {

        ProcessoNotificacaoDTO processoNotificacao = obter(idProcessoNotificacao);

        if (processoNotificacao.getCausaNaoDoacao() == null && processoNotificacao.getEntrevista().isDoacaoAutorizada()) {
            return this.addNovoEstadoNoProcessoNotificacao(processoNotificacao, EstadoNotificacaoEnum.AGUARDANDOCAPTACAO, idFuncionario);
        }

        return this.addNovoEstadoNoProcessoNotificacao(processoNotificacao, EstadoNotificacaoEnum.AGUARDANDOARQUIVAMENTO, idFuncionario);
    }


    /**
     * Quando um processo de notificacao etapa óbito esta em analise
     * uma das opcoes é arquivar o processo,
     * logo, o estado muda para NOTIFICACAOARQUIVADA.
     */
    public Long arquivarProcesso(ProcessoNotificacaoDTO processoNotificacaoDTO, Long idFuncionario) {

        ProcessoNotificacao notificacao = mapearProcessoNotificacaoDTO(processoNotificacaoDTO);
        if (notificacao.getCausaNaoDoacao().getNome() == null
                || notificacao.getCausaNaoDoacao().getTipoNaoDoacao() == null) {
            notificacao.setCausaNaoDoacao(null);
        }
        arquivar(notificacao);

        return this.addNovoEstadoNoProcessoNotificacao(processoNotificacaoDTO, EstadoNotificacaoEnum.NOTIFICACAOARQUIVADA, idFuncionario);
    }

    /**
     * Dado haja causa de nao doacao ou o processo de notificacao chegou ao fim,
     * o estado do mesmo deve mudar para AGUARDANDOARQUIVAMENTO.
     */
    public Long finalizarProcesso(Long idProcessoNotificacao, Long idFuncionario) {

        ProcessoNotificacaoDTO processoNotificacao = obter(idProcessoNotificacao);

        return this.addNovoEstadoNoProcessoNotificacao(processoNotificacao, EstadoNotificacaoEnum.AGUARDANDOARQUIVAMENTO, idFuncionario);
    }

    private Long addNovoEstadoNoProcessoNotificacao(ProcessoNotificacaoDTO processoNotificacaoDTO,
                                                    EstadoNotificacaoEnum enumEstado,
                                                    Long idFuncionario) {

        ProcessoNotificacao notificacao = mapearProcessoNotificacaoDTO(processoNotificacaoDTO);

        this.addNovoEstado(enumEstado, notificacao, idFuncionario);

        notificacaoRepository.save(notificacao);

        return notificacao.getId();
    }

    private ProcessoNotificacao mapearProcessoNotificacaoDTO(ProcessoNotificacaoDTO processoNotificacaoDTO) {

        return mapper.map(processoNotificacaoDTO, ProcessoNotificacao.class);
    }

    public void addNovoEstado(EstadoNotificacaoEnum enumEstado, ProcessoNotificacao processo, Long idFuncionario) {

        AtualizacaoEstado novoEstado = new AtualizacaoEstado();

        novoEstado.setDataAtualizacaos(Calendar.getInstance());
        novoEstado.setEstadoNotificacao(enumEstado);
        novoEstado.setFuncionario(this.getFuncionario(idFuncionario));

        processo.mudarEstadoAtual(novoEstado);
    }

    /**
     * Adiciona o historio de estados na notificacao, linkando o primeiro estado
     *
     * @param notificacao Notificacao a ser adicionado o estado inicial.
     */
    private void addEstadoInicial(ProcessoNotificacao notificacao, Long idFuncionario) {
        AtualizacaoEstado atualizacaoEstado = new AtualizacaoEstado();

        atualizacaoEstado.setFuncionario(this.getFuncionario(idFuncionario));
        atualizacaoEstado.setDataAtualizacaos(Calendar.getInstance());
        atualizacaoEstado.setEstadoNotificacao(EstadoNotificacaoEnum.AGUARDANDOANALISEOBITO);

        notificacao.mudarEstadoAtual(atualizacaoEstado);
    }

    /**
     * Retorna um funcionario dado um id recebido.
     * OBS.: O funcionario nao tem todos os campos preenchidos, apenas o ID
     *
     * @return - Retorna um funcionario
     */
    private Funcionario getFuncionario(Long idFuncionario) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(idFuncionario);
        return funcionario;
    }

    /**
     * Obtém todos os Processos de Notificacao.
     *
     * @return Uma lista de {@code ProcessoNotificacaoDTO}.
     */
    public List<ProcessoNotificacao> obterTodasNotificacoes() {
        return notificacaoRepository.findAll();
    }

    /**
     * Metodo que arquiva uma notificacao
     *
     * @param notificacao Notificacao - Notificacao que sofera o arquivamoento
     */
    public void arquivar(ProcessoNotificacao notificacao) {
        notificacao.setArquivado(true);
        notificacaoRepository.save(notificacao);
    }

    /**
     * Obter Processo de Notificacao
     *
     * @param id - Id do Processo de Notificacao
     */
    public ProcessoNotificacaoDTO obter(Long id) {
        ProcessoNotificacao processoNotificacao = getProcessoNotificacao(id);

        return mapper.map(processoNotificacao, ProcessoNotificacaoDTO.class);
    }

    public ProcessoNotificacao getProcessoNotificacao(Long id) {
        return notificacaoRepository.findOne(id);
    }

    /**
     * Metodo que busca todas as notificacoes nao arquivadas
     *
     * @return List - Lista de notificacoes
     */
    public List<ProcessoNotificacaoDTO> retornarNotificacaoNaoArquivada() {
        List<ProcessoNotificacao> processoNotificacaos = notificacaoRepository
                .findByDataArquivamentoIsNullOrderByDataAberturaDesc();

        return utility.mapList(processoNotificacaos,
                ProcessoNotificacaoDTO.class);
    }

    /**
     * Metodo que busca todas as notificacoes arquivadas
     *
     * @return List - Lista de notificacoes
     */
    @SuppressWarnings("unused")
    public List<ProcessoNotificacaoDTO> retornarNotificacaoArquivada() {
        List<ProcessoNotificacao> processoNotificacaos = notificacaoRepository
                .findByDataArquivamentoIsNotNullOrderByDataAberturaDesc();

        return utility.mapList(processoNotificacaos,
                ProcessoNotificacaoDTO.class);
    }

    /**
     * Metodo que busca todas as notificacoes por data inicio e final
     *
     * @param DataAberturaIni Calendar - Data inicial da busca
     * @param DataAberturaFim Calendar - Data final da busca
     * @return List - Lista de notificacoes
     */
    @SuppressWarnings("unused")
    public List<ProcessoNotificacaoDTO> retornarNotificacaoPorData(
            Calendar DataAberturaIni, Calendar DataAberturaFim) {
        List<ProcessoNotificacao> processoNotificacaos = notificacaoRepository
                .findByDataAberturaBetween(DataAberturaIni, DataAberturaFim);

        return utility.mapList(processoNotificacaos,
                ProcessoNotificacaoDTO.class);
    }

    /**
     * Metodo que busca todas as notificacoes, pegando apenas as que estão no
     * estado atual indicado.
     *
     * @param estado Estado que será usado para filtrar as notificações.
     * @return Notificações filtras pelo estado atual.
     */
    public List<ProcessoNotificacaoDTO> retornarNotificacaoPorEstadoAtual(
            EstadoNotificacaoEnum estado) {
        List<ProcessoNotificacao> processosNotificacao = notificacaoRepository
                .findByUltimoEstadoEstadoNotificacaoOrderByUltimoEstadoDataAtualizacaosAsc(estado);

        return utility.mapList(processosNotificacao,
                ProcessoNotificacaoDTO.class);
    }

    /**
     * Metodo que busca todas as notificacoes, pegando apenas as que estão no
     * estado atual indicado.
     *
     * @param estado Estado que será usado para filtrar as notificações.
     * @return Notificações filtras pelo estado atual.
     */
    public List<ProcessoNotificacao> retornarProcessoNotificacaoPorEstadoAtual(
            EstadoNotificacaoEnum estado) {

        return notificacaoRepository
                .findByUltimoEstadoEstadoNotificacaoOrderByUltimoEstadoDataAtualizacaosAsc(estado);
    }

    /**
     * Busca todos os Processos de Notificações com apenas uma string,
     * pelo codigo do processo, nome do Notificador, Nome do Pciente e Nome da Mãe do Paciente
     *
     * @param search - String para busca
     * @return
     */
    public List<ProcessoNotificacao> obterTodasNotificacoes(String search) {
        return notificacaoRepository.findByCodigoIgnoreCaseContainingOrNotificadorNomeIgnoreCaseContainingOrObitoPacienteNomeIgnoreCaseContainingOrObitoPacienteNomeMaeIgnoreCaseContaining(search, search, search, search);
    }

    public void recusarAnaliseCaptacao(Long idProcesso, Long idFuncionario) {
        ProcessoNotificacaoDTO processoNotificacao = obter(idProcesso);

        this.addNovoEstadoNoProcessoNotificacao(
                processoNotificacao,
                EstadoNotificacaoEnum.AGUARDANDOCORRECAOCAPTACACAO,
                idFuncionario);
    }

    public void excluirProcesso(Long idProcesso) {
        ProcessoNotificacao processo = mapper.map(obter(idProcesso), ProcessoNotificacao.class);

        processo.setAtivo(false);
        notificacaoRepository.save(processo);
    }

    public void confirmarAnaliseCaptacao(Long idProcesso, Long idFuncionario) {
        ProcessoNotificacaoDTO processoNotificacao = obter(idProcesso);

        this.addNovoEstadoNoProcessoNotificacao(
                processoNotificacao,
                EstadoNotificacaoEnum.AGUARDANDOARQUIVAMENTO,
                idFuncionario);
    }

    public List<ProcessoNotificacao> obterPorPacienteNome(String searchString) {
        return notificacaoRepository.findByObitoPacienteNomeContainingAndEntrevistaIsNotNullAndEntrevistaDoacaoAutorizadaTrue(searchString);
    }

    public void arquivarProcessoNotificacao(Long id, Long idFuncionario) {
        ProcessoNotificacaoDTO processoNotificacao = obter(id);

        this.addNovoEstadoNoProcessoNotificacao(
                processoNotificacao,
                EstadoNotificacaoEnum.NOTIFICACAOARQUIVADA,
                idFuncionario);
    }
}
