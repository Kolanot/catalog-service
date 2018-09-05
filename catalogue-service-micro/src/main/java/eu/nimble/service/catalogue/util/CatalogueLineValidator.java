package eu.nimble.service.catalogue.util;

import com.google.common.base.Strings;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 08-Aug-18.
 */
public class CatalogueLineValidator {

    private List<String> errorMessages;
    private CatalogueType owningCatalogue;
    private CatalogueLineType catalogueLine;
    private String extractedLineId;

    public CatalogueLineValidator(CatalogueType catalogueType, CatalogueLineType catalogueLine) {
        this(catalogueType, catalogueLine, new ArrayList<>());
    }

    public CatalogueLineValidator(CatalogueType catalogueType, CatalogueLineType catalogueLine, List<String> errorMessages) {
        this.owningCatalogue = catalogueType;
        this.errorMessages = errorMessages;
        this.catalogueLine = catalogueLine;
    }

    public List<String> validate() {
        // set the ID to be used during the subsequent validations
        extractedLineId = !Strings.isNullOrEmpty(catalogueLine.getID()) ? catalogueLine.getID() : catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID();

        idExists();
        manufacturerIdExists();
        lineIdManufacturerIdMatches();
        nameExists();
        commodityClassificationExists();
        partyIdsMatch();

        return  errorMessages;
    }

    private void idExists() {
        if (extractedLineId == null) {
            errorMessages.add(String.format("No id set for catalogue line."));
        }
    }

    private void manufacturerIdExists() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (Strings.isNullOrEmpty(item.getManufacturerParty().getID())) {
            errorMessages.add(String.format("No manufacturer party id set for catalogue line: %s", extractedLineId));
        }
    }

    private void lineIdManufacturerIdMatches() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (!Strings.isNullOrEmpty(catalogueLine.getID()) && !Strings.isNullOrEmpty(item.getManufacturersItemIdentification().getID())) {
            if(!catalogueLine.getID().contentEquals(item.getManufacturersItemIdentification().getID())) {
                errorMessages.add(String.format("Catalogue line id and manufacturer id do not match. line id: %s, manufacturer id: %s", extractedLineId, item.getManufacturersItemIdentification().getID()));
            }
        }
    }

    private void nameExists() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (Strings.isNullOrEmpty(item.getName())) {
            errorMessages.add(String.format("No name set for catalogue line. id: %s", extractedLineId));
        }
    }

    private void commodityClassificationExists() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (item.getCommodityClassification().size() == 0) {
            errorMessages.add(String.format("No commodity classification is set for catalogue line. id: %s", extractedLineId));
        }
    }

    private void partyIdsMatch() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        String catalogueProviderPartyId = owningCatalogue.getProviderParty().getID();
        String itemManufacturerPartyId = item.getManufacturerParty().getID();
        if (!catalogueProviderPartyId.contentEquals(itemManufacturerPartyId)) {
            errorMessages.add(String.format("Catalogue provider party and manufacturer party ids do no match for catalogue line. id: %s, catalogue provider party id: %s, line manufacturer party id: %s", extractedLineId, catalogueProviderPartyId, itemManufacturerPartyId));
        }
    }
}