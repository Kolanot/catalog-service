package eu.nimble.service.catalogue.config.interceptor;

import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.validation.IValidationUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This interceptor injects the bearer token into the {@link ExecutionContext} for each Rest call
 *
 * Created by suat on 24-Jan-19.
 */
@Configuration
public class RestServiceInterceptor extends HandlerInterceptorAdapter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ExecutionContext executionContext;
    @Autowired
    IValidationUtil iValidationUtil;

    private final String swaggerPath = "swagger-resources";
    private final String apiDocsPath = "api-docs";
    private final String CLAIMS_FIELD_REALM_ACCESS = "realm_access";
    private final String CLAIMS_FIELD_ROLES = "roles";

    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response, Object handler) {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        Claims claims = null;
        // do not validate the token for swagger operations
        if(bearerToken != null && !(request.getServletPath().contains(swaggerPath) || request.getServletPath().contains(apiDocsPath))){
            // validate token
            try {
                claims = iValidationUtil.validateToken(bearerToken);
            } catch (Exception e) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_NO_USER_FOR_TOKEN.toString(), Arrays.asList(bearerToken),e);
            }
        }

        // set token to the execution context
        executionContext.setBearerToken(bearerToken);
        // set user's available roles to the execution context
        if(claims != null){
            LinkedHashMap realmAccess = (LinkedHashMap) claims.get(CLAIMS_FIELD_REALM_ACCESS);
            List<String> roles = (List<String>) realmAccess.get(CLAIMS_FIELD_ROLES);

            executionContext.setUserRoles(roles);
        }
        // save the time as an Http attribute
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // calculate and log the execution time for the request
        long startTime = (Long)request.getAttribute("startTime");

        long endTime = System.currentTimeMillis();

        long executionTime = endTime - startTime;
        if(executionContext.getRequestLog() != null){
            logger.info("Duration for '{}' is {} millisecond",executionContext.getRequestLog(),executionTime);
        }
    }
}
