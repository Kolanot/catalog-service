package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test01_CatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();
    private static String createdCatalogueId;

    @Test
    public void test1_postJsonCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        createdCatalogueId = catalogue.getUUID();
    }

    @Test
    public void test2_getJsonCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(createdCatalogueId, catalogue.getUUID());
    }

    @Test
    public void test3_updateJsonCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId);
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        // update party address
        catalogue.getProviderParty().getPostalAddress().setCityName("Luleburgaz");

        // get Json version of the updated catalogue
        String catalogueTypeAsString = mapper.writeValueAsString(catalogue);

        // make request
        request = put("/catalogue/ubl")
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueTypeAsString);
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        // check whether it is updated or not
        Assert.assertEquals("Luleburgaz", catalogue.getProviderParty().getPostalAddress().getCityName());
    }

    @Test
    public void test4_deleteCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = delete("/catalogue/ubl/" + createdCatalogueId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        request = get("/catalogue/ubl/" + createdCatalogueId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNoContent()).andReturn();
    }

    @Test
    public void test5_getJsonNonExistingCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNoContent()).andReturn();
    }
}
