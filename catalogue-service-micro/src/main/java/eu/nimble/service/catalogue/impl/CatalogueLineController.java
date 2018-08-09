package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.util.CatalogueLineValidator;
import eu.nimble.service.catalogue.util.CatalogueValidator;
import eu.nimble.service.catalogue.util.HttpResponseUtil;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.config.CatalogueServiceConfig;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 22-Aug-17.
 * <p>
 * Catalogue line-level REST services. All services defined in this class are prefixed with the
 * "/catalogue/{catalogueUuid}/catalogueline" path where the {@catalogueId} is the unique identifier of the specified
 * catalogue. All services in this class work only on the UBL based catalogues.
 */
@Controller
@RequestMapping(value = "/catalogue/{catalogueUuid}/catalogueline")
public class CatalogueLineController {
    private static Logger log = LoggerFactory.getLogger(CatalogueLineController.class);

    @Autowired
    private CatalogueServiceConfig catalogueServiceConfig;

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    /**
     * Retrieves the catalogue line specified with the {@code lineId} parameter
     *
     * @param catalogueUuid
     * @param lineId
     * @return <li>200 along with the requested catalogue line</li>
     * <li>204 if there does not exists a catalogue line with the given lineId</li>
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve the catalogue line specified with the catalogueUuid and lineId parameters")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue line successfully", response = CatalogueLineType.class),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 400, message = "Failed to get catalogue line"),
            @ApiResponse(code = 204, message = "There does not exist a catalogue line with the given lineId")
    })
    @RequestMapping(value = "/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLine(@PathVariable String catalogueUuid, @PathVariable String lineId) {
        log.info("Incoming request to get catalogue line with lineId: {}", lineId);

        if (service.getCatalogue(catalogueUuid) == null) {
            log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
        }

        CatalogueLineType catalogueLine;
        try {
            catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get catalogue line", HttpStatus.BAD_REQUEST, e);
        }

        if (catalogueLine == null) {
            log.error("There does not exist a catalogue line with lineId {}", lineId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(String.format("There does not exist a catalogue line with lineId %s", lineId));
        }
        log.info("Completed the request to get catalogue line with lineId: {}", lineId);
        return ResponseEntity.ok(catalogueLine);
    }

    /**
     * Adds the provided line
     *
     * @param catalogueUuid
     * @param catalogueLineJson
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Add the provided line")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Added the catalogue line successfully"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 406, message = "There already exists a product with the given id"),
            @ApiResponse(code = 400, message = "Failed to deserialize catalogue line from json string"),
            @ApiResponse(code = 500, message = "Failed to add the provided catalogue line")
    })
    @RequestMapping(
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addCatalogueLine(@PathVariable String catalogueUuid, @RequestBody String catalogueLineJson) {
        log.info("Incoming request to add catalogue line to catalogue: {}", catalogueUuid);
        CatalogueType catalogue;
        CatalogueLineType catalogueLine;

        try {
            // get owning catalogue
            catalogue = service.getCatalogue(catalogueUuid);
            if (catalogue == null) {
                log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
            }

            // parse catalogue line
            try {
                catalogueLine = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(catalogueLineJson, CatalogueLineType.class);
            } catch (IOException e) {
                log.warn("The following catalogue line could not be created: {}", catalogueLineJson);
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to deserialize catalogue line: %s", catalogueLineJson), e, HttpStatus.BAD_REQUEST, LogLevel.ERROR);
            }

            // validate the incoming content
            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogue, catalogueLine);
            List<String> errors = catalogueLineValidator.validate();
            if (errors.size() > 0) {
                if (errors.size() > 0) {
                    StringBuilder sb = new StringBuilder("");
                    for (String error : errors) {
                        sb.append(error).append(System.lineSeparator());
                    }
                    return HttpResponseUtil.createResponseEntityAndLog(sb.toString(), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
                }
            }

            // check duplicate line
            boolean lineExists = service.existCatalogueLineById(catalogueUuid, catalogueLine.getID(), catalogueLine.getHjid());
            if (!lineExists) {
                catalogueLine = service.addLineToCatalogue(catalogue, catalogueLine);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("There already exists a product with the given id");
            }

        } catch (Exception e) {
            log.warn("The following catalogue line could not be created: {}", catalogueLineJson);
            return createErrorResponseEntity("Failed to add the provided catalogue line", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed request to add catalogue line: {} to catalogue: {}", catalogueLine.getID(), catalogue.getUUID());
        return createCreatedCatalogueLineResponse(catalogueUuid, catalogueLine);
    }

    private ResponseEntity createCreatedCatalogueLineResponse(String catalogueUuid, CatalogueLineType line) {
        URI lineURI;
        try {
            String applicationUrl = catalogueServiceConfig.getSpringApplicationUrl();
            lineURI = new URI(applicationUrl + "/catalogue/" + catalogueUuid + "/" + line.getID());
        } catch (URISyntaxException e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.warn(msg, e);
            try {
                log.info("Completed request to add catalogue line with an empty URI, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
                return ResponseEntity.created(new URI("")).body(line);
            } catch (URISyntaxException e1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("WTF");
            }
        }
        log.info("Completed request to add catalogue, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
        return ResponseEntity.created(lineURI).body(line);
    }

    /**
     * Updates the catalogue line
     *
     * @param catalogueUuid
     * @param catalogueLineJson updated catalogue line information
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Update the catalogue line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the catalogue line successfully"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 406, message = "There already exists a product with the given id"),
            @ApiResponse(code = 400, message = "Failed to deserialize catalogue line from json string"),
            @ApiResponse(code = 500, message = "Failed to add the provided catalogue line")
    })
    @RequestMapping(consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogueLine(@PathVariable String catalogueUuid, @RequestBody String catalogueLineJson) {
        try {
            log.info("Incoming request to update catalogue line. Catalogue uuid: {}", catalogueUuid);
            CatalogueType catalogue = service.getCatalogue(catalogueUuid);
            if (catalogue == null) {
                log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
            }

            CatalogueLineType catalogueLine;

            //parse catalogue line
            try {
                catalogueLine = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(catalogueLineJson, CatalogueLineType.class);

            } catch (IOException e) {
                log.warn("The following catalogue line could not be updated: {}", catalogueLineJson);
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to deserialize catalogue line from json string: %s", catalogueLineJson), e, HttpStatus.BAD_REQUEST, LogLevel.ERROR);
            }

            // validate the incoming content
            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogue, catalogueLine);
            List<String> errors = catalogueLineValidator.validate();
            if (errors.size() > 0) {
                if (errors.size() > 0) {
                    StringBuilder sb = new StringBuilder("");
                    for (String error : errors) {
                        sb.append(error).append(System.lineSeparator());
                    }
                    return HttpResponseUtil.createResponseEntityAndLog(sb.toString(), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
                }
            }

            // consider the case of an updated line id conflicting with the id of an existing line
            boolean lineExists = service.existCatalogueLineById(catalogueUuid, catalogueLine.getID(), catalogueLine.getHjid());
            if (!lineExists) {
                service.updateCatalogueLine(catalogueLine);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("There already exists a product with the given id");
            }

            log.info("Completed the request to add catalogue line catalogue uuid, line lineId: {}", catalogueUuid, catalogueLine.getID());
            return ResponseEntity.ok(catalogueLine);

        } catch (Exception e) {
            log.warn("The following catalogue line could not be updated: {}", catalogueLineJson);
            return createErrorResponseEntity("Failed to add the provided catalogue line", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Deletes the specified catalogue line
     *
     * @param catalogueUuid
     * @param lineId
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Delete the specified catalogue line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the catalogue line successfully"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 500, message = "Failed to delete the catalogue line")
    })
    @RequestMapping(value = "/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogueLineById(@PathVariable String catalogueUuid, @PathVariable String lineId) {
        log.info("Incoming request to delete catalogue line. catalogue uuid: {}: line lineId {}", catalogueUuid, lineId);

        if (service.getCatalogue(catalogueUuid) == null) {
            log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
        }

        try {
            service.deleteCatalogueLineById(catalogueUuid, lineId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to delete the catalogue line. catalogue uuid: " + catalogueUuid + " line id: " + lineId, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        log.info("Completed the request to delete catalogue line: catalogue uuid: {}, lineId: {}", catalogueUuid, lineId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status, Exception e) {
        msg = msg + e.getMessage();
        log.error(msg, e);
        return ResponseEntity.status(status).body(msg);
    }
}
