/* SPDX-License-Identifier: BSD-3-Clause */

package it.mds.sdk.flusso.sism.semiresidenziale.parser.regole;

import it.mds.sdk.libreriaregole.dtos.RecordDtoGenerico;
import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordDtoSismSemiresidenziale extends RecordDtoGenerico {

    //ANN_RIF~COD_PER~COD_REG~COD_ASR_RFR~COD_DSM~ID_REC_KEY~COD_STR~ID_CNT~TIP_STR_SEM_RES~DAT_ITV~MDL_PZA~TIP_TRS

    @CsvBindByPosition(position = 0)
    private String annoRiferimento; //ANN_RIF

    @CsvBindByPosition(position = 1)
    private String periodoRiferimento; //COD_PER

    @CsvBindByPosition(position = 2)
    private String codiceRegione; //COD_REG

    @CsvBindByPosition(position = 3)
    private String codiceAziendaSanitariaRiferimento; //COD_ASR_RFR

    @CsvBindByPosition(position = 4)
    private String codiceDipartimentoSaluteMentale;//COD_DSM

    @CsvBindByPosition(position = 5)
    private String idRecord;//ID_REC_KEY

    @CsvBindByPosition(position = 6)
    private String codiceStruttura;//COD_STR

    @CsvBindByPosition(position = 7)
    private Long idContatto; //ID_CNT

    @CsvBindByPosition(position = 8)
    private String tipoStrutturaSemiresidenziale; //TIP_STR_SEM_RES

    @CsvBindByPosition(position = 9)
    private String dataIntervento; //DAT_ITV

    @CsvBindByPosition(position = 10)
    private String modalitaPresenza; //MDL_PZA

    @CsvBindByPosition(position = 11)
    private String tipoOperazionePrestazione;//TIP_TRS

}
