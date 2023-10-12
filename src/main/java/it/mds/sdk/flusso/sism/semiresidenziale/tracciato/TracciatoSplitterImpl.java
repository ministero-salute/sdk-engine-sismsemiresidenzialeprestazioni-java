package it.mds.sdk.flusso.sism.semiresidenziale.tracciato;

import it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.RecordDtoSismSemiresidenziale;
import it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.conf.ConfigurazioneFlussoSismSemires;
import it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory;
import it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.PeriodoRiferimento;
import it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.SemiResidenzialePrestazioniSanitarie;
import it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.TipoOperazione;
import it.mds.sdk.libreriaregole.tracciato.TracciatoSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component("tracciatoSplitterSismSemiRes")
public class TracciatoSplitterImpl implements TracciatoSplitter<RecordDtoSismSemiresidenziale> {

    private static final String XML_FILENAME_TEMPLATE = "SDK_RES_PSS_%s_%s.xml" ;

    @Override
    public List<Path> dividiTracciato(Path tracciato) {
        return null;
    }

    @Override
    public List<Path> dividiTracciato(List<RecordDtoSismSemiresidenziale> records, String idRun) {

        try {
            ConfigurazioneFlussoSismSemires conf = getConfigurazione();
            String annoRif = records.get(0).getAnnoRiferimento();
            String codiceRegione = records.get(0).getCodiceRegione();


            //XML PRESTAZIONI
            it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni = getObjectFactory();
            SemiResidenzialePrestazioniSanitarie residenzialePrestazioniSanitarie = objPrestazioni.createSemiResidenzialePrestazioniSanitarie();
            residenzialePrestazioniSanitarie.setAnnoRiferimento(annoRif);
            residenzialePrestazioniSanitarie.setCodiceRegione(codiceRegione);
            residenzialePrestazioniSanitarie.setPeriodoRiferimento(PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));

            for (RecordDtoSismSemiresidenziale r : records) {
                if (!r.getTipoOperazionePrestazione().equalsIgnoreCase("NM")) {
                    creaPrestazioniXml(r, residenzialePrestazioniSanitarie, objPrestazioni);
                }
            }

            //recupero il path del file xsd di prestazioni
            URL resourcePrestazioni = this.getClass().getClassLoader().getResource("PSS.xsd");
            log.debug("URL dell'XSD per la validazione idrun {} : {}", idRun, resourcePrestazioni);

            //scrivi XML PRESTAZIONI
            //String pathPrestazioni = conf.getXmlOutput().getPercorso() + "SDK_RES_PSS_" + records.get(0).getPeriodoRiferimento() + "_" + idRun + ".xml";
            String fileName = String.format(XML_FILENAME_TEMPLATE,records.get(0).getPeriodoRiferimento(), idRun);
            Path xml = Path.of(conf.getXmlOutput().getPercorso(),fileName);
            return List.of(xml);
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            log.error("[{}].dividiTracciato  - records[{}]  - idRun[{}] -" + e.getMessage(),
                    this.getClass().getName(),
                    e
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossibile validare il csv in ingresso. message: " + e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void creaPrestazioniXml(RecordDtoSismSemiresidenziale r, SemiResidenzialePrestazioniSanitarie semiResidenzialePrestazioniSanitarie,
                                    it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {


        //ASL RIF
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento currentAsl = getCurrentAsl(semiResidenzialePrestazioniSanitarie, r);
        if (currentAsl == null) {
            currentAsl = creaAslPrestazioni(r.getCodiceAziendaSanitariaRiferimento(), objPrestazioni);
            semiResidenzialePrestazioniSanitarie.getAziendaSanitariaRiferimento().add(currentAsl);

        }

        //DSM
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM currentDsm = getCurrentDsm(currentAsl, r);
        if (currentDsm == null) {
            currentDsm = creaDSMPrestazioni(r.getCodiceDipartimentoSaluteMentale(), objPrestazioni);
            currentAsl.getDSM().add(currentDsm);
        }

        //ASSISTITO
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito currentAssisitito = getCurrentAssistito(currentDsm, r);
        if (currentAssisitito == null) {
            currentAssisitito = creaAssistitoPrestazioni(r, objPrestazioni);
            currentDsm.getAssistito().add(currentAssisitito);
        }

        //STRUTTURA
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura currentStruttura = getCurrentStruttura(currentAssisitito, r);
        if (currentStruttura == null) {
            currentStruttura = creaStrutturaPrestazioni(r, objPrestazioni);
            currentAssisitito.getStruttura().add(currentStruttura);
        }

        //CONTATTO

        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto currentContatto = getCurrentContatto(currentStruttura, r);
        if (currentContatto == null) {
            currentContatto = creaContattoPrestazioni(r, objPrestazioni);
            currentStruttura.getContatto().add(currentContatto);
        }

        //PRESTAZIONI
        currentContatto.getPrestazioni().add(creaPrestazioni(r, objPrestazioni));


    }

    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento creaAslPrestazioni(String codAsl,
                                                                                                it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento asl = objPrestazioni.createSemiResidenzialePrestazioniSanitarieAziendaSanitariaRiferimento();
        asl.setCodiceAziendaSanitariaRiferimento(codAsl);
        return asl;
    }

    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM creaDSMPrestazioni(String codDsm,
                                                                                                    it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM dsm = objPrestazioni.createSemiResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSM();
        dsm.setCodiceDSM(codDsm);
        return dsm;
    }

    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito creaAssistitoPrestazioni(RecordDtoSismSemiresidenziale r,
                                                                                                                    it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito assistito = objPrestazioni.createSemiResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistito();
        assistito.setIdRec(r.getIdRecord());
        return assistito;
    }

    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura creaStrutturaPrestazioni(RecordDtoSismSemiresidenziale r,
                                                                                                                              it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura struttura = objPrestazioni.createSemiResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistitoStruttura();
        struttura.setCodiceStruttura(r.getCodiceStruttura());
        return struttura;
    }

    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto creaContattoPrestazioni(RecordDtoSismSemiresidenziale r,
                                                                                                                                      it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto contatto = objPrestazioni.createSemiResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistitoStrutturaContatto();
        contatto.setIDContatto(r.getIdContatto());
        return contatto;
    }

    private SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto.Prestazioni creaPrestazioni(RecordDtoSismSemiresidenziale r,
                                                                                                                                          it.mds.sdk.flusso.sism.semiresidenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto.Prestazioni prestazioni = objPrestazioni.createSemiResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistitoStrutturaContattoPrestazioni();
        XMLGregorianCalendar dataIntervento = null;
        try {
            dataIntervento = r.getDataIntervento() != null ? DatatypeFactory.newInstance().newXMLGregorianCalendar(r.getDataIntervento()) : null;
        } catch (DatatypeConfigurationException e) {
            log.error("Errore conversione XMLGregorianCalendar date", e);
        }
        prestazioni.setDataIntervento(dataIntervento);
        prestazioni.setTipoOperazione(TipoOperazione.fromValue(r.getTipoOperazionePrestazione()));
        prestazioni.setModalitaPresenza(r.getModalitaPresenza());
        prestazioni.setTipoStrutturaSemiresidenziale(r.getTipoStrutturaSemiresidenziale());

        return prestazioni;
    }

    public SemiResidenzialePrestazioniSanitarie creaSemiResidenzialePrestazioniSanitarie(List<RecordDtoSismSemiresidenziale> records, SemiResidenzialePrestazioniSanitarie semiResidenzialePrestazioniSanitarie) {

        //Imposto gli attribute element
        String annoRif = records.get(0).getAnnoRiferimento();
        String codiceRegione = records.get(0).getCodiceRegione();

        if (semiResidenzialePrestazioniSanitarie == null) {
            ObjectFactory objPrestazioni = getObjectFactory();
            semiResidenzialePrestazioniSanitarie = objPrestazioni.createSemiResidenzialePrestazioniSanitarie();
            semiResidenzialePrestazioniSanitarie.setAnnoRiferimento(annoRif);
            semiResidenzialePrestazioniSanitarie.setCodiceRegione(codiceRegione);
            semiResidenzialePrestazioniSanitarie.setPeriodoRiferimento(PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));


            for (RecordDtoSismSemiresidenziale r : records) {
                if (!r.getTipoOperazionePrestazione().equalsIgnoreCase("NM")) {
                    creaPrestazioniXml(r, semiResidenzialePrestazioniSanitarie, objPrestazioni);
                }
            }

        }
        return semiResidenzialePrestazioniSanitarie;
    }

    public SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM getCurrentDsm(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento currentAsl, RecordDtoSismSemiresidenziale r) {
        return currentAsl.getDSM()
                .stream()
                .filter(dsm -> r.getCodiceDipartimentoSaluteMentale().equalsIgnoreCase(dsm.getCodiceDSM()))
                .findFirst()
                .orElse(null);
    }

    public SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento getCurrentAsl(SemiResidenzialePrestazioniSanitarie residenzialeAnagrafica, RecordDtoSismSemiresidenziale r) {
        return residenzialeAnagrafica.getAziendaSanitariaRiferimento()
                .stream()
                .filter(asl -> r.getCodiceAziendaSanitariaRiferimento().equalsIgnoreCase(asl.getCodiceAziendaSanitariaRiferimento()))
                .findFirst()
                .orElse(null);
    }

    public SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto getCurrentContatto(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura currentStruttura, RecordDtoSismSemiresidenziale r) {
        return currentStruttura.getContatto()
                .stream()
                .filter(cnt -> r.getIdContatto().equals(cnt.getIDContatto()))
                .findFirst()
                .orElse(null);
    }

    public SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura getCurrentStruttura(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito currentAssisitito, RecordDtoSismSemiresidenziale r) {
        return currentAssisitito.getStruttura()
                .stream()
                .filter(str -> r.getCodiceStruttura().equalsIgnoreCase(str.getCodiceStruttura()))
                .findFirst()
                .orElse(null);
    }

    public SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito getCurrentAssistito(SemiResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM currentDsm, RecordDtoSismSemiresidenziale r) {
        return currentDsm.getAssistito()
                .stream()
                .filter(ass -> r.getIdRecord().equalsIgnoreCase(ass.getIdRec()))
                .findFirst()
                .orElse(null);
    }

    public ConfigurazioneFlussoSismSemires getConfigurazione() {
        return new ConfigurazioneFlussoSismSemires();
    }

    private ObjectFactory getObjectFactory() {
        return new ObjectFactory();
    }
}
