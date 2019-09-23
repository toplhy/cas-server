package org.jasig.cas.web.flow;

import org.jasig.cas.authentication.Credential;
import org.jasig.cas.authentication.CustomUsernamePasswordCredential;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.webflow.execution.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Component("customAuthenticationViaFormAction")
public class CustomAuthenticationViaFormAction extends AuthenticationViaFormAction{

    /**
     * 验证码校验
     * @param context
     * @param credential
     * @param messageContext
     * @return
     * @throws Exception
     */
    public final String validatorCode(final RequestContext context, final Credential credential, final MessageContext messageContext){
        final HttpServletRequest request = WebUtils.getHttpServletRequest(context);
        HttpSession session = request.getSession();

        CustomUsernamePasswordCredential upc = (CustomUsernamePasswordCredential) credential;
        //检查用户名是否为空
        if (!org.springframework.util.StringUtils.hasText(upc.getUsername())) {
            messageContext.addMessage(new MessageBuilder().error().code("username.required").build());
            return "error";
        }

        //检查密码是否为空
        if (!org.springframework.util.StringUtils.hasText(upc.getPassword())) {
            messageContext.addMessage(new MessageBuilder().error().code("password.required").build());
            return "error";
        }

        //输入的验证码
        String captcha = upc.getCaptcha();
        //在session中获取生成的验证码
        String sessionCaptcha = (String) session.getAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);
        session.removeAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);

        //验证验证码是否存在
        if (!StringUtils.hasText(captcha) || !StringUtils.hasText(sessionCaptcha)) {
            messageContext.addMessage(new MessageBuilder().error().code("captcha.required").build());
            return "error";
        }
        // 判断验证码是否正确
        if (captcha.equalsIgnoreCase(sessionCaptcha)) {
            return "success";
        }
        messageContext.addMessage(new MessageBuilder().error().code("error.authentication.captcha.bad").build());
        return "error";
    }
}
