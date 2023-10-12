package it.mds.sdk.flusso.sism.semiresidenziale;

import it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.RecordDtoSismSemiresidenziale;
import it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.conf.ConfigurazioneFlussoSismSemires;
import it.mds.sdk.flusso.sism.semiresidenziale.tracciato.TracciatoSplitterImpl;
import it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory;
import it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.SemiResidenzialePrestazioniSanitarie;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.dtos.CampiInputBean;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@MockitoSettings(strictness = Strictness.LENIENT)
public class TracciatoSplitterImplTest {

    @InjectMocks
    @Spy
    private TracciatoSplitterImpl tracciatoSplitter;
    private ConfigurazioneFlussoSismSemires configurazione = Mockito.mock(ConfigurazioneFlussoSismSemires.class);
    private ObjectFactory objectFactory = Mockito.mock(ObjectFactory.class);
    private SemiResidenzialePrestazioniSanitarie residenzialePrestazioniSanitarie = Mockito.mock(SemiResidenzialePrestazioniSanitarie.class);
    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento asl = Mockito.mock(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.class);
    private ConfigurazioneFlussoSismSemires.XmlOutput xmlOutput = Mockito.mock(ConfigurazioneFlussoSismSemires.XmlOutput.class);
    private MockedStatic<GestoreFileFactory> gestore;
    private GestoreFile gestoreFile = Mockito.mock(GestoreFile.class);
    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento aziendaSanitariaRiferimento = Mockito.mock(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.class);
    private List<SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento> aziendaSanitariaRiferimentoList = new ArrayList<>();
    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM dsm = Mockito.mock(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.class);
    private List<SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM> listDsm = new ArrayList<>();
    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito assistito = Mockito.mock(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.class);
    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura struttura = Mockito.mock(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.class);
    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto contatto = Mockito.mock(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto.class);
    private RecordDtoSismSemiresidenziale r = new RecordDtoSismSemiresidenziale();
    List<RecordDtoSismSemiresidenziale> records = new ArrayList<>();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        gestore = mockStatic(GestoreFileFactory.class);
        initMockedRecord(r);
        records.add(r);
    }

    @Test
    void dividiTracciatoTest() throws JAXBException, IOException, SAXException {

        when(tracciatoSplitter.getConfigurazione()).thenReturn(configurazione);
        when(objectFactory.createSemiResidenzialePrestazioniSanitarie()).thenReturn(residenzialePrestazioniSanitarie);
        when(residenzialePrestazioniSanitarie.getAziendaSanitariaRiferimento()).thenReturn(List.of(asl));
        when(configurazione.getXmlOutput()).thenReturn(xmlOutput);
        when(xmlOutput.getPercorso()).thenReturn("percorso");
        gestore.when(() -> GestoreFileFactory.getGestoreFile("XML")).thenReturn(gestoreFile);
        doNothing().when(gestoreFile).scriviDto(any(), any(), any());

        Assertions.assertEquals(
                List.of(Path.of("percorso","SDK_RES_PSS_S1_100.xml")),
                this.tracciatoSplitter.dividiTracciato(records, "100")
        );

    }

    @Test
    void dividiTracciatoTestOk2() throws JAXBException, IOException, SAXException {
        records.get(0).setTipoOperazionePrestazione("C");
        when(tracciatoSplitter.getConfigurazione()).thenReturn(configurazione);
        when(objectFactory.createSemiResidenzialePrestazioniSanitarie()).thenReturn(residenzialePrestazioniSanitarie);
        when(residenzialePrestazioniSanitarie.getAziendaSanitariaRiferimento()).thenReturn(List.of(asl));

        when(configurazione.getXmlOutput()).thenReturn(xmlOutput);
        when(xmlOutput.getPercorso()).thenReturn("percorso");
        gestore.when(() -> GestoreFileFactory.getGestoreFile("XML")).thenReturn(gestoreFile);
        doNothing().when(gestoreFile).scriviDto(any(), any(), any());

        doReturn(null).when(tracciatoSplitter).getCurrentAsl(any(), any());
        doReturn(null).when(tracciatoSplitter).getCurrentDsm(any(), any());
        doReturn(null).when(tracciatoSplitter).getCurrentAssistito(any(), any());
        doReturn(null).when(tracciatoSplitter).getCurrentStruttura(any(), any());
        doReturn(null).when(tracciatoSplitter).getCurrentContatto(any(), any());

        Assertions.assertEquals(
                List.of(Path.of("percorso","SDK_RES_PSS_S1_100.xml")),
                this.tracciatoSplitter.dividiTracciato(records, "100")
        );

    }

    @Test
    void getContattoTest() {
        var list = List.of(contatto);

        when(struttura.getContatto()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentContatto(struttura, r);
    }

    @Test
    void getStrutturaTest() {
        var list = List.of(assistito);

        when(dsm.getAssistito()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentAssistito(dsm, r);
    }

    @Test
    void getCurrentDsmTest() {
        var list = List.of(dsm);
        when(asl.getDSM()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentDsm(asl, r);
    }

    @Test
    void getCurrentAslTest() {
        var list = List.of(asl);

        when(residenzialePrestazioniSanitarie.getAziendaSanitariaRiferimento()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentAsl(residenzialePrestazioniSanitarie, r);
    }

    @Test
    void creaPrestazioniSanitarieTest() {
        var list = List.of(asl);
        var c = tracciatoSplitter.creaSemiResidenzialePrestazioniSanitarie(records, null);
    }

    @AfterEach
    void closeMocks() {
        gestore.close();
    }

    private void initMockedRecord(RecordDtoSismSemiresidenziale r) {
        CampiInputBean campiInputBean = new CampiInputBean();
        campiInputBean.setPeriodoRiferimentoInput("Q1");
        campiInputBean.setAnnoRiferimentoInput("2022");
        r.setAnnoRiferimento("2022");
        r.setCodiceRegione("080");
        r.setPeriodoRiferimento("S1");
        r.setTipoOperazionePrestazione("NM");
        r.setCodiceDipartimentoSaluteMentale("cdsm");
        r.setCodiceAziendaSanitariaRiferimento("casr");
        r.setIdContatto(92L);
        r.setCodiceStruttura("cs");
        r.setIdRecord("ic");
        records.add(r);
    }
}
