package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.catalogue.model.catalogue.CataloguePaginationResponse;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 31-Dec-18.
 */
public class CataloguePersistenceUtil {
    private static final String QUERY_GET_BY_UUID = "SELECT catalogue FROM CatalogueType catalogue WHERE catalogue.UUID = :uuid";
    private static final String QUERY_GET_FOR_PARTY = "SELECT catalogue FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";
    private static final String QUERY_CHECK_EXISTENCE_BY_ID = "SELECT COUNT(catalogue) FROM CatalogueType catalogue"
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification"
            + " WHERE catalogue.ID = :catalogueId and partyIdentification.ID = :partyId";
    private static final String QUERY_GET_CATALOGUE_IDS_FOR_PARTY = "SELECT catalogue.UUID FROM CatalogueType as catalogue" +
            " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification" +
            " WHERE partyIdentification.ID = :partyId";

    private static final String QUERY_GET_CATALOGUE_LINES_BY_IDS = "SELECT catalogueLine FROM CatalogueLineType catalogueLine " +
            " WHERE catalogueLine.ID in :catalogueLineIds";
    private static final String QUERY_GET_COMMODITY_CLASSIFICATION_NAMES_OF_CATALOGUE_LINES = "SELECT DISTINCT itemClassificationCode.name FROM CatalogueType as catalogue " +
            " JOIN catalogue.catalogueLine catalogueLine JOIN catalogueLine.goodsItem.item.commodityClassification commodityClassification JOIN commodityClassification.itemClassificationCode itemClassificationCode " +
            " WHERE catalogue.UUID = :catalogueUuid";
    private static final String QUERY_GET_CATALOGUE_LINE_IDS_FOR_PARTY = "SELECT catalogueLine.ID FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification JOIN catalogue.catalogueLine catalogueLine"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";

    private static final String QUERY_GET_CATALOGUE_LINE_IDS_WITH_CATEGORY_NAME_FOR_PARTY = "SELECT catalogueLine.ID FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification JOIN catalogue.catalogueLine catalogueLine "
            + " JOIN catalogueLine.goodsItem.item.commodityClassification commodityClassification JOIN commodityClassification.itemClassificationCode itemClassificationCode "
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId"
            + " AND itemClassificationCode.name in :categoryName";
    private static final String QUERY_GET_CATALOGUE_LINE_COUNT_FOR_PARTY = "SELECT COUNT(catalogueLine) FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification JOIN catalogue.catalogueLine catalogueLine"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";
    private static final String QUERY_GET_CATALOGUE_UUID_FOR_PARTY = "SELECT catalogue.UUID FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";
    // native queries
    private static final String QUERY_GET_CATALOGUE_LINE_IDS_WITH_CATEGORY_NAME_AND_SEARCH_TEXT_FOR_PARTY = "select catalogue_line.id from catalogue_type catalogue join party_type party on (catalogue.provider_party_catalogue_typ_0 = party.hjid)" +
            " join party_identification_type party_identification on (party_identification.party_identification_party_t_0 = party.hjid)" +
            " join catalogue_line_type catalogue_line on (catalogue_line.catalogue_line_catalogue_typ_0 = catalogue.hjid)" +
            " join goods_item_type goods_item on (catalogue_line.goods_item_catalogue_line_ty_0 = goods_item.hjid)" +
            " join item_type item_type on (goods_item.item_goods_item_type_hjid = item_type.hjid)" +
            " join text_type text_type on (text_type.name__item_type_hjid = item_type.hjid or text_type.description_item_type_hjid = item_type.hjid)" +
            " join commodity_classification_type commodity_classification on (commodity_classification.commodity_classification_ite_0 = item_type.hjid)" +
            " join code_type code_type on (code_type.hjid = commodity_classification.item_classification_code_com_0)" +
            " join item_location_quantity_type item_location on (catalogue_line.required_item_location_quant_1 = item_location.hjid)" +
            " join price_type price_type on (item_location.price_item_location_quantity_0 = price_type.hjid)" +
            " join amount_type amount_type on (price_type.price_amount_price_type_hjid = amount_type.hjid)" +
            " where catalogue.id = :catalogueId and party_identification.id = :partyId and code_type.name_ = :categoryName and text_type.language_id = :languageId and text_type.value_ in (" +
            " SELECT text_type.value_ FROM text_type, plainto_tsquery(:searchText) AS q WHERE (value_ @@ q)" +
            ")";

    private static final String QUERY_GET_CATALOGUE_LINE_IDS_WITH_SEARCH_TEXT_FOR_PARTY = "select catalogue_line.id from catalogue_type catalogue join party_type party on (catalogue.provider_party_catalogue_typ_0 = party.hjid)" +
            " join party_identification_type party_identification on (party_identification.party_identification_party_t_0 = party.hjid)" +
            " join catalogue_line_type catalogue_line on (catalogue_line.catalogue_line_catalogue_typ_0 = catalogue.hjid)" +
            " join goods_item_type goods_item on (catalogue_line.goods_item_catalogue_line_ty_0 = goods_item.hjid)" +
            " join item_type item_type on (goods_item.item_goods_item_type_hjid = item_type.hjid)" +
            " join text_type text_type on (text_type.name__item_type_hjid = item_type.hjid or text_type.description_item_type_hjid = item_type.hjid)" +
            " join item_location_quantity_type item_location on (catalogue_line.required_item_location_quant_1 = item_location.hjid)" +
            " join price_type price_type on (item_location.price_item_location_quantity_0 = price_type.hjid)" +
            " join amount_type amount_type on (price_type.price_amount_price_type_hjid = amount_type.hjid) " +
            " where catalogue.id = :catalogueId and party_identification.id = :partyId and text_type.language_id = :languageId and text_type.value_ in (" +
            " SELECT text_type.value_ FROM text_type, plainto_tsquery(:searchText) AS q WHERE (value_ @@ q)" +
            ")";

    public static CataloguePaginationResponse getCatalogueLinesForParty(String catalogueId, String partyId, String selectedCategoryName, String searchText, String languageId, CatalogueLineSortOptions sortOption, int limit, int offset) {
        // get catalogue uuid
        String catalogueUuid = new JPARepositoryFactory().forCatalogueRepository(false).getSingleEntity(QUERY_GET_CATALOGUE_UUID_FOR_PARTY,new String[]{"catalogueId","partyId"}, new Object[]{catalogueId,partyId});
        long size = 0;
        List<String> categoryNames = new ArrayList<>();
        List<CatalogueLineType> catalogueLines = new ArrayList<>();
        if(catalogueUuid != null){
            // get number of catalogue lines which the catalogue contains
            size = new JPARepositoryFactory().forCatalogueRepository(false).getSingleEntity(QUERY_GET_CATALOGUE_LINE_COUNT_FOR_PARTY,new String[]{"catalogueId","partyId"}, new Object[]{catalogueId,partyId});
            // get names of the categories for all catalogue lines which the catalogue contains
            categoryNames = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(QUERY_GET_COMMODITY_CLASSIFICATION_NAMES_OF_CATALOGUE_LINES,new String[]{"catalogueUuid"}, new Object[]{catalogueUuid});
            // if limit is equal to 0,then no catalogue lines are returned
            if(limit != 0){
                // get the query
                QueryData queryData = getQuery(catalogueId,partyId,searchText,languageId,selectedCategoryName);
                // get catalogue line ids according to the given limit and offset
                List<String> catalogueLineIds = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(queryData.query,queryData.parameterNames.toArray(new String[0]), queryData.parameterValues.toArray(),limit,offset,queryData.isNativeQuery);

                // check whether we need to consider any sort options
                // if we do, then update the query
                String getCatalogueLinesQuery = QUERY_GET_CATALOGUE_LINES_BY_IDS;
                if(sortOption != null){
                    switch (sortOption){
                        case PRICE_HIGH_TO_LOW:
                            getCatalogueLinesQuery += " ORDER BY catalogueLine.requiredItemLocationQuantity.price.priceAmount.value DESC NULLS LAST";
                            break;
                        case PRICE_LOW_TO_HIGH:
                            getCatalogueLinesQuery += " ORDER BY catalogueLine.requiredItemLocationQuantity.price.priceAmount.value ASC";
                            break;
                    }
                }

                if(catalogueLineIds.size() != 0)
                    catalogueLines = new JPARepositoryFactory().forCatalogueRepository().getEntities(getCatalogueLinesQuery,new String[]{"catalogueLineIds"}, new Object[]{catalogueLineIds});
            }
        }
        // created CataloguePaginationResponse
        CataloguePaginationResponse cataloguePaginationResponse = new CataloguePaginationResponse();
        cataloguePaginationResponse.setSize(size);
        cataloguePaginationResponse.setCatalogueLines(catalogueLines);
        cataloguePaginationResponse.setCatalogueUuid(catalogueUuid);
        cataloguePaginationResponse.setCategoryNames(categoryNames);
        return cataloguePaginationResponse;
    }

    public static CatalogueType getCatalogueByUuid(String catalogueUuid) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_BY_UUID, new String[]{"uuid"}, new Object[]{catalogueUuid});
    }

    public static CatalogueType getCatalogueForParty(String catalogueId, String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_FOR_PARTY, new String[]{"catalogueId", "partyId"}, new Object[]{catalogueId, partyId});
    }

    public static Boolean checkCatalogueExistenceById(String catalogueId, String partyId) {
        long catalogueExists = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_ID, new String[]{"catalogueId", "partyId"}, new Object[]{catalogueId, partyId});
        return catalogueExists == 1 ? true : false;
    }

    public static List<String> getCatalogueIdsForParty(String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_CATALOGUE_IDS_FOR_PARTY, new String[]{"partyId"}, new Object[]{partyId});
    }

    private static QueryData getQuery(String catalogueId,String partyId,String searchText,String languageId,String selectedCategoryName){
        QueryData queryData = new QueryData();

        // catalogue id and party id are the common parameters for all queries
        queryData.parameterNames.add("catalogueId");
        queryData.parameterValues.add(catalogueId);

        queryData.parameterNames.add("partyId");
        queryData.parameterValues.add(partyId);

        // no category name filtering and search text filtering
        if(selectedCategoryName == null && searchText == null){
            queryData.query = QUERY_GET_CATALOGUE_LINE_IDS_FOR_PARTY;
            queryData.isNativeQuery = false;
        }
        // category name filtering and search text filtering
        else if(selectedCategoryName != null && searchText != null){
            queryData.parameterNames.add("languageId");
            queryData.parameterValues.add(languageId);

            queryData.parameterNames.add("searchText");
            queryData.parameterValues.add(searchText);

            queryData.parameterNames.add("categoryName");
            queryData.parameterValues.add(selectedCategoryName);

            queryData.query = QUERY_GET_CATALOGUE_LINE_IDS_WITH_CATEGORY_NAME_AND_SEARCH_TEXT_FOR_PARTY;
            queryData.isNativeQuery = true;
        }
        // category name filtering
        else if(selectedCategoryName != null){
            queryData.parameterNames.add("categoryName");
            queryData.parameterValues.add(selectedCategoryName);

            queryData.query = QUERY_GET_CATALOGUE_LINE_IDS_WITH_CATEGORY_NAME_FOR_PARTY;
            queryData.isNativeQuery = false;
        }
        // search text filtering
        else{
            queryData.parameterNames.add("languageId");
            queryData.parameterValues.add(languageId);

            queryData.parameterNames.add("searchText");
            queryData.parameterValues.add(searchText);

            queryData.query = QUERY_GET_CATALOGUE_LINE_IDS_WITH_SEARCH_TEXT_FOR_PARTY;
            queryData.isNativeQuery = true;
        }
        return queryData;
    }

    private static class QueryData {
        private String query;
        private List<String> parameterNames = new ArrayList<>();
        private List<Object> parameterValues = new ArrayList<>();
        boolean isNativeQuery;
    }
}