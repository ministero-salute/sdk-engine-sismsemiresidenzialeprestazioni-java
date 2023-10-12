package it.mds.sdk.flusso.sism.semiresidenziale.controller;

import it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.RecordDtoSismSemiresidenziale;
import it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.conf.ConfigurazioneFlussoSismSemires;
import it.mds.sdk.flusso.sism.semiresidenziale.service.FlussoSismSemiresService;
import it.mds.sdk.gestoreesiti.GestoreRunLog;
import it.mds.sdk.gestoreesiti.Progressivo;
import it.mds.sdk.gestoreesiti.modelli.InfoRun;
import it.mds.sdk.gestoreesiti.modelli.StatoRun;
import it.mds.sdk.gestoreesiti.modelli.TipoElaborazione;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.parser.ParserRegole;
import it.mds.sdk.libreriaregole.parser.ParserTracciato;
import it.mds.sdk.libreriaregole.regole.beans.RegoleFlusso;
import it.mds.sdk.rest.api.controller.ValidazioneController;
import it.mds.sdk.rest.persistence.entity.FlussoRequest;
import it.mds.sdk.rest.persistence.entity.RecordRequest;
import it.mds.sdk.rest.persistence.entity.RisultatoInizioValidazione;
import it.mds.sdk.rest.persistence.entity.RisultatoValidazione;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.sql.Timestamp;

@RestController
@Configuration
@EnableAsync
@Slf4j
public class FlussoSismSemiResidenzialeControllerRest implements ValidazioneController<RecordDtoSismSemiresidenziale> {

    private static final String FILE_CSV = "CSV";


    private final ParserRegole parserRegole;
    private final ParserTracciato parserTracciato;
    private final FlussoSismSemiresService flussoSismSemiresService;
    private final ConfigurazioneFlussoSismSemires conf;

    private final MultiValueMap<String, String> headers;

    @Autowired
    public FlussoSismSemiResidenzialeControllerRest(@Qualifier("parserRegoleSismSemiRes") final ParserRegole parserRegole,
                                                    @Qualifier("parserTracciatoSismSemiRes") final ParserTracciato parserTracciato,
                                                    @Qualifier("flussoSismSemiResService") final FlussoSismSemiresService flussoSismSemiresService,
                                                    @Qualifier("configurazioneFlussoSismSemiRes") ConfigurazioneFlussoSismSemires conf) {
        this.parserRegole = parserRegole;
        this.parserTracciato = parserTracciato;
        this.flussoSismSemiresService = flussoSismSemiresService;
        this.conf = conf;

        headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("X-XSS-Protection", "1; mode=block");
        headers.add("Content-Security-Policy", "default-src 'self'");

    }

    @Override
    @PostMapping(path = "v1/flusso/sismsemiresidenzialeprestazioni")
    public ResponseEntity<RisultatoInizioValidazione> validaTracciato(@RequestBody FlussoRequest flusso, String nomeFlussoController) {
        String filename = FilenameUtils.normalize(flusso.getNomeFile());
        log.debug("{}.validaTracciato - BEGIN", this.getClass().getName());
        if (flusso.getAnnoRiferimento() == null || flusso.getAnnoRiferimento().isEmpty()
                || flusso.getPeriodoRiferimento() == null || flusso.getPeriodoRiferimento().isEmpty()
                || flusso.getCodiceRegione() == null || flusso.getCodiceRegione().isEmpty()) {
            return new ResponseEntity<RisultatoInizioValidazione>(new RisultatoInizioValidazione(false, "Campi obbligatori Mancanti", "", flusso.getIdClient()), headers, HttpStatus.BAD_REQUEST);
        }

        log.debug("{}.validaTracciato - annoRiferimento[{}] - periodoRiferimento[{}] - codiceRegione[{}] ", this.getClass().getName(), flusso.getCodiceRegione(), flusso.getPeriodoRiferimento(), flusso.getAnnoRiferimento());
        File tracciato = getFileFromPath(conf.getFlusso().getPercorso() + filename);
        if (!tracciato.exists()) {
            log.debug("File tracciato {} non trovato ", conf.getFlusso().getPercorso() + filename);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File tracciato non trovato");
        }

        File fileRegole = getFileFromPath(conf.getRules().getPercorso());
        if (!fileRegole.exists()) {
            log.debug("File regole {} non trovato ", conf.getRules().getPercorso());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File Regole non trovato");
        }
        RegoleFlusso regoleFlusso = getRegoleFlusso(fileRegole);


        GestoreFile gestoreFile = GestoreFileFactory.getGestoreFile(FILE_CSV);
        GestoreRunLog gestoreRunLog = getGestoreRunLog(gestoreFile, Progressivo.creaProgressivo(Progressivo.Fonte.FILE));
        String nomeFlusso = conf.getNomeFLusso().getNomeFlusso();
        InfoRun infoRun = gestoreRunLog.creaRunLog(flusso.getIdClient(), flusso.getModalitaOperativa(), 0, nomeFlusso);
        infoRun.setTipoElaborazione(TipoElaborazione.F);
        infoRun.setTimestampCreazione(new Timestamp(System.currentTimeMillis()));
        infoRun.setVersion(getClass().getPackage().getImplementationVersion());
        infoRun.setAnnoRiferimento(flusso.getAnnoRiferimento());
        infoRun.setPeriodoRiferimento(flusso.getPeriodoRiferimento());
        infoRun.setCodiceRegione(flusso.getCodiceRegione());
        infoRun.setFileAssociatiRun(filename);
        gestoreRunLog.updateRun(infoRun);
        infoRun = gestoreRunLog.cambiaStatoRun(infoRun.getIdRun(), StatoRun.IN_ELABORAZIONE);
        int dimensioneBlocco = Integer.parseInt(conf.getDimensioneBlocco().getDimensioneBlocco());
        flussoSismSemiresService.validazioneBlocchi(dimensioneBlocco, flusso.getNomeFile(), regoleFlusso, infoRun.getIdRun(),
                flusso.getIdClient(), flusso.getModalitaOperativa(),
                flusso.getPeriodoRiferimento(), flusso.getAnnoRiferimento(), flusso.getCodiceRegione(), gestoreRunLog);

        log.debug("Fine validaTracciato per idRun {}", infoRun.getIdRun());
        return new ResponseEntity<>(new RisultatoInizioValidazione(true, "", infoRun.getIdRun(), flusso.getIdClient()), headers, HttpStatus.OK);
    }

    @Override
    @PostMapping("v1/flusso/sismsemiresidenzialeprestazioni/record")
    public ResponseEntity<RisultatoValidazione> validaRecord(RecordRequest<RecordDtoSismSemiresidenziale> recordRequest, String nomeFlusso) {
        return null;
    }


    @Override
    @GetMapping("v1/flusso/sismsemiresidenzialeprestazioni/info")
    public ResponseEntity<InfoRun> informazioniRun(@RequestParam(required = false) String idRun, @RequestParam(required = false) String idClient) {
        GestoreFile gestoreFile = GestoreFileFactory.getGestoreFile(FILE_CSV);
        GestoreRunLog gestoreRunLog = getGestoreRunLog(gestoreFile, Progressivo.creaProgressivo(Progressivo.Fonte.FILE));
        InfoRun infoRun = null;
        if (idRun != null) {
            infoRun = gestoreRunLog.getRun(idRun);
        } else if (idClient != null) {
            infoRun = gestoreRunLog.getRunFromClient(idClient);
        } else {
            return new ResponseEntity<>(infoRun, headers, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(infoRun, headers, HttpStatus.OK);
    }
    public RegoleFlusso getRegoleFlusso(File fileRegole) {
        return parserRegole.parseRegole(fileRegole);
    }

    public GestoreRunLog getGestoreRunLog(GestoreFile gestoreFile, Progressivo creaProgressivo) {
        return new GestoreRunLog(gestoreFile, creaProgressivo);
    }

    public File getFileFromPath(String s) {
        return new File(s);
    }
}