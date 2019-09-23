package org.jasig.cas.web.flow;

import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.authentication.principal.WebApplicationServiceFactory;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * 在serviceCheck之前对service进行赋值
 */
@Component("setServiceBeforeCheckAction")
public class SetServiceBeforeCheckAction extends AbstractAction {

    // 没有该配置时给空
    @Value("${cas.authentication.success.noService.redirectUrl:}")
    private String defaultServiceUrl;

    @Autowired
    private WebApplicationServiceFactory webApplicationServiceFactory;

    @Override
    protected Event doExecute(RequestContext requestContext){
        // 如果不带有service，设置默认service
        if(WebUtils.getService(requestContext) == null && StringUtils.hasText(defaultServiceUrl)){
            Service service = webApplicationServiceFactory.createService(defaultServiceUrl);
            requestContext.getFlowScope().put("service", service);
        }
        return success();
    }

}
