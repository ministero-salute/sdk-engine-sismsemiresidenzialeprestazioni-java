/* SPDX-License-Identifier: BSD-3-Clause */

package it.mds.sdk.flusso.sism.semiresidenziale;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@SpringBootApplication
@ComponentScan({"it.mds.sdk.flusso.sism.semiresidenziale.controller", "it.mds.sdk.flusso.sism.semiresidenziale", "it.mds.sdk.rest.persistence.entity",
        "it.mds.sdk.libreriaregole.validator",
        "it.mds.sdk.flusso.sism.semiresidenziale.service", "it.mds.sdk.flusso.sism.semiresidenziale.tracciato",
        "it.mds.sdk.gestoreesiti", "it.mds.sdk.flusso.sism.semiresidenziale.parser.regole", "it.mds.sdk.flusso.sism.semiresidenziale.parser.regole.conf",
        "it.mds.sdk.connettoremds"})

@OpenAPIDefinition(info = @Info(title = "SDK Ministero Della Salute - Flusso PSS", version = "0.0.5-SNAPSHOT", description = "Flusso Sism SemiResidenziale"))
public class FlussoSismSemiresidenziale {

    public static void main(String[] args) {
        SpringApplication.run(FlussoSismSemiresidenziale.class, args);
    }

}
