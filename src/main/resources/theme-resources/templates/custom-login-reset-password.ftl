<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="seria" class="${properties.kcLabelClass!}">Password seria</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="seria" name="seria" class="${properties.kcInputClass!}" autofocus aria-invalid="<#if messagesPerField.existsError('seria')>true</#if>"/>
                    <#if messagesPerField.existsError('seria')>
                        <span id="input-error-number" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('seria'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="number" class="${properties.kcLabelClass!}">Password number</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="number" name="number" class="${properties.kcInputClass!}" autofocus aria-invalid="<#if messagesPerField.existsError('number')>true</#if>"/>
                    <#if messagesPerField.existsError('number')>
                        <span id="input-error-number" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('number'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
