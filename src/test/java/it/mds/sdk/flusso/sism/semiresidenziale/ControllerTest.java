package it.mds.sdk.flusso.sism.semiresidenziale;

import it.mds.sdk.flusso.sism.semiresidenziale.controller.FlussoSismSemiResidenzialeControllerRest;
import it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.conf.ConfigurazioneFlussoSismSemires;
import it.mds.sdk.flusso.sism.semiresidenziale.service.FlussoSismSemiresService;
import it.mds.sdk.gestoreesiti.GestoreRunLog;
import it.mds.sdk.gestoreesiti.modelli.InfoRun;
import it.mds.sdk.gestoreesiti.modelli.ModalitaOperativa;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.parser.ParserRegole;
import it.mds.sdk.libreriaregole.regole.beans.RegoleFlusso;
import it.mds.sdk.rest.persistence.entity.FlussoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@MockitoSettings(strictness = Strictness.LENIENT)
public class ControllerTest {

    @InjectMocks
    @Spy
    private FlussoSismSemiResidenzialeControllerRest controller;

    private FlussoRequest flussoRequest = new FlussoRequest();

    private MockedStatic<GestoreFileFactory> gestoreFileFactory;

    @Spy
    private ConfigurazioneFlussoSismSemires conf;
    @Spy
    private ParserRegole parserRegole;
    @Mock
    private FlussoSismSemiresService service;
    private ConfigurazioneFlussoSismSemires.Rules rules = mock(ConfigurazioneFlussoSismSemires.Rules.class);

    private ConfigurazioneFlussoSismSemires.Flusso flusso = mock(ConfigurazioneFlussoSismSemires.Flusso.class);

    private File file = mock(File.class);
    private RegoleFlusso regoleFlusso = Mockito.mock(RegoleFlusso.class);
    private GestoreFile gestoreFile = Mockito.mock(GestoreFile.class);

    private GestoreRunLog gestoreRunLog = mock(GestoreRunLog.class);
    private InfoRun infoRun = Mockito.mock(InfoRun.class);
    private ConfigurazioneFlussoSismSemires.NomeFlusso nomeFlusso = Mockito.mock(ConfigurazioneFlussoSismSemires.NomeFlusso.class);


     @Test
    //tofix
    void validaTracciatoTest(){
        MockitoAnnotations.openMocks(this);
        initFlussoRequest();
        when(conf.getRules()).thenReturn(rules);
        when(rules.getPercorso()).thenReturn("percorso1_");

        when(conf.getFlusso()).thenReturn(flusso);
        when(flusso.getPercorso()).thenReturn("percorso2_");

        when(conf.getNomeFLusso()).thenReturn(nomeFlusso);
        when(nomeFlusso.getNomeFlusso()).thenReturn("nomeFlusso");
        when(controller.getFileFromPath(anyString())).thenReturn(file);
        when(file.exists()).thenReturn(true);

        gestoreFileFactory = mockStatic(GestoreFileFactory.class);
        gestoreFileFactory.when(() -> GestoreFileFactory.getGestoreFile("CSV")).thenReturn(gestoreFile);
        when(controller.getGestoreRunLog(any(), any())).thenReturn(gestoreRunLog);
        when(gestoreRunLog.creaRunLog(any(), any(), anyInt(), any())).thenReturn(infoRun);
        when(gestoreRunLog.cambiaStatoRun(any(), any())).thenReturn(infoRun);

        given(controller.getRegoleFlusso(file)).willReturn(regoleFlusso);
        when(parserRegole.parseRegole(file)).thenReturn(regoleFlusso);

        doNothing().when(service)
                .validazioneBlocchi(
                anyInt(),
                anyString(),
                any(),
                anyString(),
                anyString(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                any()
        );

        controller.validaTracciato(
                flussoRequest,
                "nomeFlusso"
        );
        gestoreFileFactory.close();
    }

    @Test
    public void informazioniRunTest(){
        MockitoAnnotations.openMocks(this);
        gestoreFileFactory = mockStatic(GestoreFileFactory.class);
        gestoreFileFactory.when(() -> GestoreFileFactory.getGestoreFile("CSV")).thenReturn(gestoreFile);
        when(controller.getGestoreRunLog(any(), any())).thenReturn(gestoreRunLog);
        when(gestoreRunLog.getRun(any())).thenReturn(infoRun);

        controller.informazioniRun("idRun","idClient");
        gestoreFileFactory.close();
    }

    private void initFlussoRequest() {
        flussoRequest.setNomeFile("nomeFile.txt");
        flussoRequest.setModalitaOperativa(ModalitaOperativa.T);
        flussoRequest.setIdClient("1");
        flussoRequest.setAnnoRiferimento("2022");
        flussoRequest.setPeriodoRiferimento("S2");
        flussoRequest.setCodiceRegione("080");
    }
}
